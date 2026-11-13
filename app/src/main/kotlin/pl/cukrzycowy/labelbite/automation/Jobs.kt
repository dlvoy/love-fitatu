package pl.cukrzycowy.labelbite.automation

import kotlinx.coroutines.flow.MutableStateFlow
import pl.cukrzycowy.labelbite.data.Meal
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

sealed interface AutomationJob {
    /** Fill Fitatu's "Szybkie dodawanie" form for [meal] on [date]; tap Zapisz when [autoSave]. */
    data class Fitatu(
        val name: String,
        val kcal: Double,
        val fat: Double,
        val carbs: Double,
        val protein: Double,
        val meal: Meal,
        val date: LocalDate,
        val autoSave: Boolean,
    ) : AutomationJob

    /** Open the AAPS bolus calculator and type [carbs] into it. Never confirms. */
    data class Aaps(val packageName: String, val carbs: Int) : AutomationJob
}

/** Last automation message, shown in the bridge app when the user returns to it. */
object AutomationStatus {
    val message = MutableStateFlow<String?>(null)
}

class StepFailed(message: String) : Exception(message)

object Format {
    /** Fitatu's Polish locale wants a decimal comma; whole numbers are written without decimals. */
    fun fitatuNumber(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else String.format(Locale.ROOT, "%.1f", rounded).replace('.', ',')
    }

    /** Text comparison for read-back: ignores whitespace and comma/point differences. */
    fun sameNumber(a: String?, b: String): Boolean =
        a != null && a.filterNot { it.isWhitespace() }.replace('.', ',') == b.filterNot { it.isWhitespace() }.replace('.', ',')
}

/**
 * Safety rules for anything the service does inside AAPS. The service may open the bolus
 * calculator and type carbs. It must never press a control that confirms or delivers anything.
 */
object AapsGuard {
    const val MAX_CARBS = 120

    val calculatorLabels = listOf("Kalkulator bolusa", "Bolus wizard", "Bolus calculator", "Calculator")
    val carbsLabels = listOf("Węglowodany", "Carbs")

    private val forbiddenParts = listOf("potwierd", "zatwierd", "confirm", "podaj", "deliver", "wyślij", "send")

    /** True for labels of controls the service must never click (Potwierdź, OK, Confirm, ...). */
    fun isForbiddenLabel(label: CharSequence?): Boolean {
        val s = label?.toString()?.trim()?.lowercase() ?: return false
        return s == "ok" || forbiddenParts.any { s.contains(it) }
    }

    fun isCalculatorLabel(label: CharSequence?): Boolean =
        label != null && calculatorLabels.any { it.equals(label.toString().trim(), ignoreCase = true) }

    fun isCarbsLabel(label: CharSequence?): Boolean =
        label != null && carbsLabels.any { it.equals(label.toString().trim(), ignoreCase = true) }

    fun validCarbs(carbs: Int): Boolean = carbs in 1..MAX_CARBS
}
