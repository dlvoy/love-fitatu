package pl.lovefitatu.bridge.data

import android.content.Context

/**
 * App configuration. Everything except the model-list cache is kept in [SecureStore], so reads and
 * writes are only valid once the store is unlocked.
 */
class Settings(context: Context) {
    private val context = context.applicationContext
    private val cache = this.context.getSharedPreferences("cache", Context.MODE_PRIVATE)

    init {
        // Leftover from the first build, which kept settings in plain preferences.
        this.context.deleteSharedPreferences("settings")
    }

    /** Setting a key turns on biometric protection of the whole configuration; clearing it turns it off. */
    var apiKey: String
        get() = SecureStore["api_key"].orEmpty()
        set(value) {
            SecureStore.put(context, "api_key", value.trim().ifEmpty { null })
            SecureStore.setBiometric(context, value.isNotBlank())
        }

    var model: String
        get() = SecureStore["model"] ?: DEFAULT_MODEL
        set(value) = SecureStore.put(context, "model", value.trim().ifBlank { null })

    /** Last model the user picked from the list; shown in the picker's preferred section. */
    var lastPickedModel: String
        get() = SecureStore["last_picked_model"].orEmpty()
        set(value) = SecureStore.put(context, "last_picked_model", value)

    var autoSave: Boolean
        get() = SecureStore["auto_save"] != "false"
        set(value) = SecureStore.put(context, "auto_save", value.toString())

    var mealMapping: String
        get() = SecureStore["meal_mapping"] ?: MealMapping.DEFAULT
        set(value) = SecureStore.put(context, "meal_mapping", value.ifBlank { null })

    var aapsPackage: String
        get() = SecureStore["aaps_package"] ?: DEFAULT_AAPS_PACKAGE
        set(value) = SecureStore.put(context, "aaps_package", value.trim().ifBlank { null })

    /** History view: photo grid when true, list with details when false. */
    var galleryGrid: Boolean
        get() = SecureStore["gallery_grid"] != "false"
        set(value) = SecureStore.put(context, "gallery_grid", value.toString())

    /** Public model catalogue from OpenRouter (not secret), in ModelCatalog.encode format. */
    var cachedModels: String
        get() = cache.getString("models", null).orEmpty()
        set(value) = cache.edit().putString("models", value).apply()

    val biometricProtected: Boolean get() = SecureStore.isBiometricMode(context)
    val biometricAvailable: Boolean get() = SecureStore.biometricAvailable(context)

    companion object {
        const val DEFAULT_MODEL = "google/gemini-2.5-flash"
        const val DEFAULT_AAPS_PACKAGE = "info.nightscout.androidaps"
        const val FITATU_PACKAGE = "com.fitatu.tracker"
    }
}
