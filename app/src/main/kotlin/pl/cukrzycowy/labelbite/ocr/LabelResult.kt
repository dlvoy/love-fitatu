package pl.cukrzycowy.labelbite.ocr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

/** What the vision model reads off a Love Catering label. All macro values are for the whole meal. */
@Serializable
data class LabelResult(
    val day: String? = null,
    val code: String? = null,
    @SerialName("dish_name") val dishName: String? = null,
    val kcal: Double? = null,
    @SerialName("fat_g") val fat: Double? = null,
    @SerialName("carbs_g") val carbs: Double? = null,
    @SerialName("protein_g") val protein: Double? = null,
) {
    /** True when the macros do not add up to the stated energy within 15 %. */
    fun energyMismatch(): Boolean {
        val k = kcal ?: return false
        val computed = 4 * (carbs ?: return false) + 4 * (protein ?: return false) + 9 * (fat ?: return false)
        return k > 0 && abs(computed - k) / k > 0.15
    }

    /** The `[detected]` section of a history file; unread fields are left out. */
    fun toToml(): Map<String, Any> = buildMap {
        day?.let { put("day", it) }
        code?.let { put("code", it) }
        dishName?.let { put("dish_name", it) }
        kcal?.let { put("kcal", it) }
        fat?.let { put("fat_g", it) }
        carbs?.let { put("carbs_g", it) }
        protein?.let { put("protein_g", it) }
    }

    companion object {
        fun fromToml(section: Map<String, Any>) = LabelResult(
            day = section["day"]?.toString(),
            code = section["code"]?.toString(),
            dishName = section["dish_name"]?.toString(),
            kcal = (section["kcal"] as? Number)?.toDouble(),
            fat = (section["fat_g"] as? Number)?.toDouble(),
            carbs = (section["carbs_g"] as? Number)?.toDouble(),
            protein = (section["protein_g"] as? Number)?.toDouble(),
        )

        private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

        /** Parses the model reply, tolerating code fences or prose around the JSON object. */
        fun parse(reply: String): LabelResult {
            val start = reply.indexOf('{')
            val end = reply.lastIndexOf('}')
            require(start in 0 until end) { "Brak obiektu JSON w odpowiedzi modelu" }
            return json.decodeFromString(serializer(), reply.substring(start, end + 1))
        }
    }
}
