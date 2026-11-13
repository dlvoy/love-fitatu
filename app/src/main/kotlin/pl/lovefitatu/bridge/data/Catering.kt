package pl.lovefitatu.bridge.data

import pl.lovefitatu.bridge.R

/** A diet catering whose labels the app reads. [tintIcon] is false for full-colour logos. */
enum class Catering(
    val id: String,
    val label: String,
    val icon: Int,
    val tintIcon: Boolean,
    val siteUrl: String?,
    val defaultMealMapping: String,
) {
    LOVE("love", "Love Catering", R.drawable.logo_love_catering, false, "https://lovecatering.pl/", MealMapping.DEFAULT),
    CUSTOM("custom", "Własny", R.drawable.ic_edit_note, true, null, MealMapping.DEFAULT);

    companion object {
        fun byId(id: String?): Catering = entries.firstOrNull { it.id == id } ?: LOVE
    }
}

/** The text sent to the model with the photo. Every variant asks for the same JSON keys. */
object LabelPrompt {
    val LOVE = """
        This is a photo of a Polish diet-catering meal label (Love Catering). The photo may be rotated.
        Read it and reply with ONLY a JSON object with these keys:
        - "day": the weekday name printed on the label in Polish (e.g. "Czwartek"), or null.
          Only the weekday word; ignore every printed date ("Data produkcji", best-before dates)
        - "code": the dish code exactly as printed, in the form number/letter/letter (e.g. "10284/K/M"), or null
        - "dish_name": the dish title in Polish, as printed, or null
        - "kcal": number after "Wartość energetyczna" (kcal)
        - "fat_g": number after "Tłuszcz"
        - "carbs_g": number after "Węglowodany"
        - "protein_g": number after "Białko"
        Rules: values on the label use a decimal comma; output JSON numbers with a decimal point.
        The values are for the whole meal; copy them as printed, do not convert or recalculate.
        Do NOT confuse the main values with "W tym cukry" (sugars), "Nas. kw. tłuszczowe"
        (saturated fat) or "Błonnik" (fibre). The large handwritten-style number on the label
        is not part of the code. Use null for anything you cannot read with confidence; never guess.
    """.trimIndent()

    /** What a custom catering starts with; the user rewrites it to match their label. */
    val CUSTOM_DEFAULT = """
        Etykieta posiłku z cateringu dietetycznego, po polsku.
        Dzień tygodnia jest wydrukowany słownie. Kod dania ma postać numer/litera/litera.
        Wartości odżywcze dotyczą całego posiłku: „Wartość energetyczna” (kcal), „Tłuszcz”, „Węglowodany”, „Białko”.
    """.trimIndent()

    /** Appended to every custom description, so the reply always has the shape the app parses. */
    val OUTPUT_RULES = """
        Read the label and reply with ONLY a JSON object with these keys:
        - "day": the weekday name the label gives for the meal, in Polish (e.g. "Czwartek"), or null.
          Only the weekday word; ignore printed dates such as production or best-before dates
        - "code": the dish code exactly as printed, or null
        - "dish_name": the dish title, as printed, or null
        - "kcal": energy of the whole meal in kcal, or null
        - "fat_g": total fat in grams, or null
        - "carbs_g": total carbohydrates in grams, or null
        - "protein_g": protein in grams, or null
        Rules: output JSON numbers with a decimal point, also when the label uses a decimal comma.
        Copy the values as printed; do not convert or recalculate. Do not confuse the main values
        with sub-values such as sugars, saturated fat or fibre.
        Use null for anything you cannot read with confidence; never guess.
    """.trimIndent()

    fun forCatering(catering: Catering, customDescription: String): String = when (catering) {
        Catering.LOVE -> LOVE
        Catering.CUSTOM -> buildString {
            append("This is a photo of a diet-catering meal label. The photo may be rotated.\n")
            append("The user describes the label as follows:\n")
            append(customDescription.trim().ifEmpty { CUSTOM_DEFAULT })
            append("\n\n")
            append(OUTPUT_RULES)
        }
    }
}
