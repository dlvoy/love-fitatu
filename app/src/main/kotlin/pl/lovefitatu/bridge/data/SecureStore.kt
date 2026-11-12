package pl.lovefitatu.bridge.data

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted configuration. All values live in one AES-GCM blob; the blob's data key is itself stored
 * wrapped by an Android Keystore key:
 *
 * - plain mode: a Keystore AES key that needs no user authentication (used while no API key is set);
 * - biometric mode: a Keystore RSA key whose private half needs a strong biometric for every use.
 *
 * Unlocking unwraps the data key once and keeps it, with the decrypted values, in this process-wide
 * object. That is why the biometric prompt appears once per app start and not again while the
 * process stays in memory, and why saving changes later needs no further prompt.
 */
object SecureStore {
    private const val PREFS = "secure"
    private const val PLAIN_ALIAS = "config_plain"
    private const val BIOMETRIC_ALIAS = "config_biometric"
    private const val MODE_BIOMETRIC = "biometric"
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

    // Keystore's OAEP uses SHA-1 for MGF1 regardless of the main digest; both sides must say so.
    private val oaep = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    private var dataKey: ByteArray? = null
    private val values = LinkedHashMap<String, String>()

    val unlocked: Boolean get() = dataKey != null

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun isBiometricMode(context: Context) = prefs(context).getString("mode", null) == MODE_BIOMETRIC

    fun biometricAvailable(context: Context): Boolean =
        context.getSystemService(BiometricManager::class.java)
            ?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    // ------------------------------------------------------------- unlocking

    /** Unlocks without a prompt. Only valid in plain mode; creates an empty store on first use. */
    fun unlockPlain(context: Context) {
        check(!isBiometricMode(context)) { "store is biometric-protected" }
        val wrapped = prefs(context).getString("wrapped_key", null)
        if (wrapped == null) {
            dataKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
            values.clear()
            rewrap(context, biometric = false)
            saveBlob(context)
            return
        }
        try {
            val raw = Base64.decode(wrapped, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, plainKey(), GCMParameterSpec(128, raw, 0, 12))
            dataKey = cipher.doFinal(raw, 12, raw.size - 12)
            loadBlob(context)
        } catch (e: Exception) {
            reset(context)
            unlockPlain(context)
        }
    }

    /**
     * Cipher to hand to BiometricPrompt as its CryptoObject. Throws (e.g.
     * KeyPermanentlyInvalidatedException after a new fingerprint is enrolled) when the key is unusable.
     */
    fun unlockCipher(): Cipher {
        val privateKey = keyStore().getKey(BIOMETRIC_ALIAS, null) as? PrivateKey ?: error("missing biometric key")
        return Cipher.getInstance(RSA_TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, privateKey, oaep) }
    }

    /** Completes a biometric unlock with the cipher the prompt authorised. */
    fun finishUnlock(context: Context, cipher: Cipher) {
        val wrapped = Base64.decode(prefs(context).getString("wrapped_key", null), Base64.NO_WRAP)
        dataKey = cipher.doFinal(wrapped)
        loadBlob(context)
    }

    /** Wipes everything: used when the stored data can no longer be decrypted. */
    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
        keyStore().apply {
            runCatching { deleteEntry(PLAIN_ALIAS) }
            runCatching { deleteEntry(BIOMETRIC_ALIAS) }
        }
        dataKey = null
        values.clear()
    }

    // ---------------------------------------------------------------- values

    operator fun get(key: String): String? {
        check(unlocked) { "SecureStore is locked" }
        return values[key]
    }

    fun put(context: Context, key: String, value: String?) {
        check(unlocked) { "SecureStore is locked" }
        if (value == null) values.remove(key) else values[key] = value
        saveBlob(context)
    }

    /**
     * Switches between plain and biometric protection of the data key. Returns the mode now in
     * effect (stays plain when the device has no strong biometric enrolled).
     */
    fun setBiometric(context: Context, on: Boolean): Boolean {
        check(unlocked) { "SecureStore is locked" }
        val target = on && biometricAvailable(context)
        if (target != isBiometricMode(context)) rewrap(context, target)
        return target
    }

    // -------------------------------------------------------------- internals

    private fun rewrap(context: Context, biometric: Boolean) {
        val key = dataKey!!
        val wrapped = if (biometric) {
            keyStore().deleteEntry(BIOMETRIC_ALIAS)
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(BIOMETRIC_ALIAS, KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                        .build()
                )
            }.generateKeyPair()
            // Re-create the public key outside the Keystore so encrypting needs no authentication.
            val encoded = keyStore().getCertificate(BIOMETRIC_ALIAS).publicKey.encoded
            val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded))
            Cipher.getInstance(RSA_TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, publicKey, oaep) }.doFinal(key)
        } else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, plainKey()) }
            cipher.iv + cipher.doFinal(key)
        }
        prefs(context).edit()
            .putString("wrapped_key", Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .putString("mode", if (biometric) MODE_BIOMETRIC else "plain")
            .apply()
        if (!biometric) runCatching { keyStore().deleteEntry(BIOMETRIC_ALIAS) }
    }

    private fun plainKey(): SecretKey {
        (keyStore().getKey(PLAIN_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(PLAIN_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    private fun saveBlob(context: Context) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(dataKey, "AES"))
        val blob = cipher.iv + cipher.doFinal(Json.encodeToString(serializer, values).toByteArray())
        prefs(context).edit().putString("blob", Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    private fun loadBlob(context: Context) {
        values.clear()
        val stored = prefs(context).getString("blob", null) ?: return
        val raw = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(dataKey, "AES"), GCMParameterSpec(128, raw, 0, 12))
        values.putAll(Json.decodeFromString(serializer, String(cipher.doFinal(raw, 12, raw.size - 12))))
    }
}
