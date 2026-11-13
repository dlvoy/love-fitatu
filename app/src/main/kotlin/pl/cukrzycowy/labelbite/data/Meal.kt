package pl.cukrzycowy.labelbite.data

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Fitatu meal sections. [fitatuKey] is the middle part of Fitatu's `planner_page_planner_<key>_section_*` ids. */
enum class Meal(val fitatuKey: String, val label: String) {
    BREAKFAST("breakfast", "Śniadanie"),
    SECOND_BREAKFAST("second_breakfast", "II śniadanie"),
    DINNER("dinner", "Obiad"),
    SNACK("snack", "Podwieczorek"),
    SUPPER("supper", "Kolacja"),
}

/** Parsed Love Catering code such as `10284/K/M`. */
data class DishCode(val dish: String, val mealLetter: String, val size: String) {
    override fun toString() = "$dish/$mealLetter/$size"

    companion object {
        private val regex = Regex("""(\d{3,})\s*/\s*(\p{L}{1,2})\s*/\s*(\p{L}{1,3})""")

        fun parse(text: String?): DishCode? {
            val m = regex.find(text ?: return null) ?: return null
            return DishCode(m.groupValues[1], m.groupValues[2].uppercase(), m.groupValues[3].uppercase())
        }
    }
}

object MealMapping {
    const val DEFAULT = "Ś=breakfast, O=dinner, P=snack, K=supper"

    /** Parses `Ś=breakfast, O=dinner` into letter → meal. Unknown entries are skipped. */
    fun parse(spec: String): Map<String, Meal> =
        spec.split(',', ';', '\n').mapNotNull { entry ->
            val (letter, key) = entry.split('=').map { it.trim() }.takeIf { it.size == 2 } ?: return@mapNotNull null
            val meal = Meal.entries.firstOrNull { it.fitatuKey.equals(key, true) || it.name.equals(key, true) }
                ?: return@mapNotNull null
            letter.uppercase() to meal
        }.toMap()

    /** Looks the letter up, tolerating a dropped diacritic (OCR reading `Ś` as `S`). */
    fun mealFor(letter: String?, spec: String): Meal? {
        if (letter.isNullOrBlank()) return null
        val map = parse(spec)
        val key = letter.trim().uppercase()
        return map[key] ?: map.entries.firstOrNull { stripDiacritics(it.key) == stripDiacritics(key) }?.value
    }
}

object LabelDate {
    private val days = mapOf(
        "poniedzialek" to DayOfWeek.MONDAY,
        "wtorek" to DayOfWeek.TUESDAY,
        "sroda" to DayOfWeek.WEDNESDAY,
        "czwartek" to DayOfWeek.THURSDAY,
        "piatek" to DayOfWeek.FRIDAY,
        "sobota" to DayOfWeek.SATURDAY,
        "niedziela" to DayOfWeek.SUNDAY,
    )

    fun dayOfWeek(name: String?): DayOfWeek? =
        days[stripDiacritics(name?.trim().orEmpty()).lowercase()]

    /**
     * The label only carries a weekday (any printed dates are production / best-before dates, not
     * the meal's day). This is that weekday within the Monday-based week containing [today], or
     * null when the name is missing or unknown.
     */
    fun inCurrentWeek(dayName: String?, today: LocalDate): LocalDate? {
        val dow = dayOfWeek(dayName) ?: return null
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(dow.value - 1L)
    }
}

fun stripDiacritics(s: String): String =
    Normalizer.normalize(s.replace('ł', 'l').replace('Ł', 'L'), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
