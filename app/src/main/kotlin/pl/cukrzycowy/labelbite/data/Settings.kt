package pl.cukrzycowy.labelbite.data

import android.content.Context
import pl.cukrzycowy.labelbite.automation.AppVersions
import pl.cukrzycowy.labelbite.automation.SteeredApp
import pl.cukrzycowy.labelbite.automation.VersionCheck
import pl.cukrzycowy.labelbite.ocr.LlmProvider

/**
 * App configuration. Everything except the model-list caches is kept in [SecureStore], so reads and
 * writes are only valid once the store is unlocked.
 *
 * Each LLM provider and each catering has its own values. OpenRouter and Love Catering use the
 * storage keys of the first release, so its configuration carries over; the others add a suffix.
 */
class Settings(context: Context) {
    private val context = context.applicationContext
    private val cache = this.context.getSharedPreferences("cache", Context.MODE_PRIVATE)

    init {
        // Leftover from the first build, which kept settings in plain preferences.
        this.context.deleteSharedPreferences("settings")
    }

    private fun key(base: String, provider: LlmProvider) =
        if (provider == LlmProvider.OPENROUTER) base else "${base}_${provider.id}"

    private fun key(base: String, catering: Catering) =
        if (catering == Catering.LOVE) base else "${base}_${catering.id}"

    // ---------------------------------------------------------- LLM provider

    var provider: LlmProvider
        get() = LlmProvider.byId(SecureStore["provider"])
        set(value) = SecureStore.put(context, "provider", value.id)

    fun apiKeyFor(provider: LlmProvider): String = SecureStore[key("api_key", provider)].orEmpty()

    /** Biometric protection of the whole configuration is on while any provider has a key. */
    fun setApiKey(provider: LlmProvider, value: String) {
        SecureStore.put(context, key("api_key", provider), value.trim().ifEmpty { null })
        SecureStore.setBiometric(context, LlmProvider.entries.any { apiKeyFor(it).isNotBlank() })
    }

    fun modelFor(provider: LlmProvider): String = SecureStore[key("model", provider)] ?: provider.defaultModel

    fun setModel(provider: LlmProvider, value: String) =
        SecureStore.put(context, key("model", provider), value.trim().ifBlank { null })

    /** Last model the user picked from the list; shown in the picker's preferred section. */
    fun lastPickedModelFor(provider: LlmProvider): String = SecureStore[key("last_picked_model", provider)].orEmpty()

    fun setLastPickedModel(provider: LlmProvider, value: String) =
        SecureStore.put(context, key("last_picked_model", provider), value)

    /** The provider's public model catalogue (not secret), in ModelCatalog.encode format. */
    fun cachedModelsFor(provider: LlmProvider): String = cache.getString(key("models", provider), null).orEmpty()

    fun setCachedModels(provider: LlmProvider, value: String) =
        cache.edit().putString(key("models", provider), value).apply()

    // -------------------------------------------------------------- catering

    var catering: Catering
        get() = Catering.byId(SecureStore["catering"])
        set(value) = SecureStore.put(context, "catering", value.id)

    fun mealMappingFor(catering: Catering): String =
        SecureStore[key("meal_mapping", catering)] ?: catering.defaultMealMapping

    fun setMealMapping(catering: Catering, value: String) =
        SecureStore.put(context, key("meal_mapping", catering), value.ifBlank { null })

    /** Code letter → meal mapping of the active catering. */
    val mealMapping: String get() = mealMappingFor(catering)

    /** The user's description of a custom catering's label. */
    var customPrompt: String
        get() = SecureStore["custom_prompt"] ?: LabelPrompt.CUSTOM_DEFAULT
        set(value) = SecureStore.put(context, "custom_prompt", value.ifBlank { null })

    /** The prompt sent with the photo for the active catering. */
    val labelPrompt: String get() = LabelPrompt.forCatering(catering, customPrompt)

    // ----------------------------------------------------------------- other

    var autoSave: Boolean
        get() = SecureStore["auto_save"] != "false"
        set(value) = SecureStore.put(context, "auto_save", value.toString())

    var aapsPackage: String
        get() = SecureStore["aaps_package"] ?: DEFAULT_AAPS_PACKAGE
        set(value) = SecureStore.put(context, "aaps_package", value.trim().ifBlank { null })

    /** Last version check of [app], or null when it was never checked. */
    fun versionCheck(app: SteeredApp): VersionCheck? = VersionCheck.decode(SecureStore[app.storageKey])

    fun setVersionCheck(app: SteeredApp, check: VersionCheck) = SecureStore.put(context, app.storageKey, check.encode())

    /**
     * Looks at what is installed as [packageName] now. The check on record, with its time, is kept
     * when it already describes that; otherwise (never checked, another package, the app was
     * updated, another verdict) the new one replaces it.
     */
    fun refreshVersionCheck(app: SteeredApp, packageName: String): VersionCheck {
        val now = AppVersions.check(context, app, packageName)
        val known = versionCheck(app)
        if (known != null && known.sameFinding(now)) return known
        setVersionCheck(app, now)
        return now
    }

    /** History view: photo grid when true, list with details when false. */
    var galleryGrid: Boolean
        get() = SecureStore["gallery_grid"] != "false"
        set(value) = SecureStore.put(context, "gallery_grid", value.toString())

    val biometricProtected: Boolean get() = SecureStore.isBiometricMode(context)
    val biometricAvailable: Boolean get() = SecureStore.biometricAvailable(context)

    companion object {
        const val DEFAULT_AAPS_PACKAGE = "info.nightscout.androidaps"
        const val FITATU_PACKAGE = "com.fitatu.tracker"
    }
}
