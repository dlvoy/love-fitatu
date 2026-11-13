package pl.cukrzycowy.labelbite.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.cukrzycowy.labelbite.automation.AapsGuard
import pl.cukrzycowy.labelbite.automation.AutomationJob
import pl.cukrzycowy.labelbite.data.DishCode
import pl.cukrzycowy.labelbite.data.LabelDate
import pl.cukrzycowy.labelbite.data.LabelRecord
import pl.cukrzycowy.labelbite.data.Meal
import pl.cukrzycowy.labelbite.data.MealMapping
import pl.cukrzycowy.labelbite.data.Settings
import pl.cukrzycowy.labelbite.ocr.LabelResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Editable copy of what the model read, plus the meal and day it will be logged under. */
class ReviewData(
    result: LabelResult,
    mealMapping: String,
    today: LocalDate,
    val photo: Bitmap?,
    /** History entry this review belongs to; null for the debug preset. */
    var record: LabelRecord? = null,
) {
    private val code = DishCode.parse(result.code)

    var name by mutableStateOf(listOfNotNull(code?.toString() ?: result.code, result.dishName).joinToString(" ").trim())
    var kcal by mutableStateOf(result.kcal.toField())
    var fat by mutableStateOf(result.fat.toField())
    var carbs by mutableStateOf(result.carbs.toField())
    var protein by mutableStateOf(result.protein.toField())
    var meal by mutableStateOf(MealMapping.mealFor(code?.mealLetter, mealMapping))
    /** Meals are logged for today unless the user picks otherwise. */
    var date by mutableStateOf(today)

    /** The label's weekday as a date in the current week; null when the label has no readable day. */
    val labelDate: LocalDate? = LabelDate.inCurrentWeek(result.day, today)
    val today: LocalDate = today

    /** True while the user still has to choose between today and a different weekday on the label. */
    var dayChoicePending by mutableStateOf(labelDate != null && labelDate != today)

    val labelDay = result.day
    val energyWarning = result.energyMismatch()

    init {
        // Reopened from history: start from what the user last sent rather than the raw detection.
        record?.section(LabelRecord.SAVED)?.takeIf { it.isNotEmpty() }?.let { saved ->
            saved["name"]?.let { name = it.toString() }
            (saved["kcal"] as? Number)?.let { kcal = it.toDouble().toField() }
            (saved["fat_g"] as? Number)?.let { fat = it.toDouble().toField() }
            (saved["carbs_g"] as? Number)?.let { carbs = it.toDouble().toField() }
            (saved["protein_g"] as? Number)?.let { protein = it.toDouble().toField() }
            Meal.entries.firstOrNull { it.fitatuKey == saved["meal"] }?.let { meal = it }
            runCatching { LocalDate.parse(saved["date"].toString()) }.getOrNull()?.let {
                date = it
                dayChoicePending = false
            }
        }
    }

    /** The `[saved]` section: the values as they stand on screen, plus where they were sent. */
    fun savedToml(sentTo: String?): Map<String, Any> = buildMap {
        put("name", name.trim())
        kcal.toNumber()?.let { put("kcal", it) }
        fat.toNumber()?.let { put("fat_g", it) }
        carbs.toNumber()?.let { put("carbs_g", it) }
        protein.toNumber()?.let { put("protein_g", it) }
        meal?.let { put("meal", it.fitatuKey); put("meal_label", it.label) }
        put("date", date.toString())
        put("saved_at", LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        sentTo?.let { put("sent_to", it) }
        if (sentTo?.contains("aaps") == true) carbsForAaps()?.let { put("aaps_carbs_g", it) }
    }

    /** Whole grams for AAPS, rounded half up; null when missing or outside what the calculator accepts. */
    fun carbsForAaps(): Int? = carbs.toNumber()?.roundToInt()?.takeIf { AapsGuard.validCarbs(it) }

    fun fitatuJob(autoSave: Boolean): AutomationJob.Fitatu? {
        return AutomationJob.Fitatu(
            name = name.trim().ifEmpty { return null },
            kcal = kcal.toNumber() ?: return null,
            fat = fat.toNumber() ?: return null,
            carbs = carbs.toNumber() ?: return null,
            protein = protein.toNumber() ?: return null,
            meal = meal ?: return null,
            date = date,
            autoSave = autoSave,
        )
    }

    private fun Double?.toField() = this?.let { String.format(Locale.ROOT, "%.1f", it).removeSuffix(".0").replace('.', ',') }.orEmpty()
    private fun String.toNumber() = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
}

private val dateFormat = DateTimeFormatter.ofPattern("EEEE d.MM", Locale.forLanguageTag("pl"))
private val dayName = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("pl"))

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    data: ReviewData,
    settings: Settings,
    status: String?,
    onRun: (List<AutomationJob>) -> Unit,
) {
    var zoom by remember { mutableStateOf(false) }
    if (zoom) ZoomableImageDialog(data.photo) { zoom = false }

    val labelDate = data.labelDate
    if (data.dayChoicePending && labelDate != null) AlertDialog(
        onDismissRequest = { data.dayChoicePending = false },
        title = { Text("Którego dnia jest ten posiłek?") },
        text = {
            Text("Na etykiecie jest ${labelDate.format(dayName)}, a dziś jest ${data.today.format(dayName)}.")
        },
        confirmButton = {
            TextButton({ data.date = data.today; data.dayChoicePending = false }) {
                Text("Dziś (${data.today.format(dateFormat)})")
            }
        },
        dismissButton = {
            TextButton({ data.date = labelDate; data.dayChoicePending = false }) {
                Text("Z etykiety (${labelDate.format(dateFormat)})")
            }
        },
    )

    var calendar by remember { mutableStateOf(false) }
    if (calendar) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = data.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { calendar = false },
            confirmButton = {
                TextButton({
                    picker.selectedDateMillis?.let {
                        data.date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    calendar = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ calendar = false }) { Text("Anuluj") } },
        ) { DatePicker(picker) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        data.photo?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Zdjęcie etykiety, dotknij aby powiększyć",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clickable { zoom = true },
            )
        }

        OutlinedTextField(data.name, { data.name = it }, label = { Text("Nazwa") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("kcal", data.kcal, Modifier.weight(1f)) { data.kcal = it }
            NumberField("Tłuszcz g", data.fat, Modifier.weight(1f)) { data.fat = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Węglowodany g", data.carbs, Modifier.weight(1f)) { data.carbs = it }
            NumberField("Białko g", data.protein, Modifier.weight(1f)) { data.protein = it }
        }
        if (data.energyWarning) Text(
            "Makroskładniki nie zgadzają się z kcal (różnica ponad 15%). Sprawdź wartości na etykiecie.",
            color = MaterialTheme.colorScheme.error,
        )

        Text("Posiłek", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.forEach { meal ->
                FilterChip(selected = data.meal == meal, onClick = { data.meal = meal }, label = { Text(meal.label) })
            }
        }
        if (data.meal == null) Text("Nie rozpoznano posiłku z kodu, wybierz ręcznie.", color = MaterialTheme.colorScheme.error)

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton({ data.date = data.date.minusDays(1) }) { Text("◀") }
            TextButton({ calendar = true }, modifier = Modifier.weight(1f)) {
                Text(data.date.format(dateFormat) + if (data.date == data.today) " (dziś)" else "")
            }
            TextButton({ data.date = data.date.plusDays(1) }) { Text("▶") }
        }
        data.labelDay?.let { Text("Dzień na etykiecie: $it", style = MaterialTheme.typography.bodySmall) }

        val fitatu = data.fitatuJob(settings.autoSave)
        val carbs = data.carbsForAaps()
        val aaps = carbs?.let { AutomationJob.Aaps(settings.aapsPackage, it) }

        Button({ onRun(listOf(fitatu!!)) }, enabled = fitatu != null, modifier = Modifier.fillMaxWidth()) {
            Text(if (settings.autoSave) "Dodaj do Fitatu" else "Wypełnij w Fitatu")
        }
        OutlinedButton({ onRun(listOf(aaps!!)) }, enabled = aaps != null, modifier = Modifier.fillMaxWidth()) {
            Text(if (carbs != null) "Kalkulator bolusa AAPS ($carbs g)" else "Kalkulator bolusa AAPS")
        }
        OutlinedButton(
            { onRun(listOf(fitatu!!, aaps!!)) },
            enabled = fitatu != null && aaps != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Fitatu, potem AAPS") }
        Text(
            "W AAPS aplikacja tylko wpisuje węglowodany. Bolus zatwierdzasz samodzielnie.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = value.isBlank(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
