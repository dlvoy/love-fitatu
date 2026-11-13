package pl.cukrzycowy.labelbite.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.cukrzycowy.labelbite.data.History
import pl.cukrzycowy.labelbite.data.LabelRecord
import pl.cukrzycowy.labelbite.ocr.ImagePrep
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.Locale

private val pl = Locale.forLanguageTag("pl")
private val weekdayName = DateTimeFormatter.ofPattern("EEEE", pl)
private val dayDate = DateTimeFormatter.ofPattern("d.MM.yyyy", pl)
private val rowTime = DateTimeFormatter.ofPattern("HH:mm", pl)
private val fullDate = DateTimeFormatter.ofPattern("EEEE d.MM.yyyy, HH:mm", pl)

fun weekTitle(weekStart: LocalDate): String = "Tydzień ${weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

/** The weekday of a day header, capitalised: "Poniedziałek". */
fun dayTitle(date: LocalDate): String = date.format(weekdayName).replaceFirstChar { it.titlecase(pl) }

private object Thumbs {
    private val cache = LruCache<String, ImageBitmap>(120)

    suspend fun load(file: File, maxEdge: Int): ImageBitmap? {
        val key = "${file.path}:${file.lastModified()}:$maxEdge"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) { ImagePrep.loadFile(file, maxEdge)?.asImageBitmap() }?.also { cache.put(key, it) }
    }
}

@Composable
private fun Thumbnail(photo: File, modifier: Modifier, maxEdge: Int = 320) {
    val image by produceState<ImageBitmap?>(null, photo) { value = Thumbs.load(photo, maxEdge) }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        image?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .background(if (selected) scheme.primary else scheme.surface.copy(alpha = 0.85f), CircleShape)
            .border(1.5.dp, if (selected) scheme.primary else scheme.outline, CircleShape),
    ) {
        if (selected) Text("✓", color = scheme.onPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WeekHeader(weekStart: LocalDate) {
    Text(
        weekTitle(weekStart),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Second level under a week: the weekday, with its date smaller on the right. */
@Composable
private fun DayHeader(date: LocalDate) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            dayTitle(date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(date.format(dayDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The last item while older entries are still unread: asks for the next page once it scrolls into view. */
@Composable
private fun LoadMore(loaded: Int, onLoadMore: () -> Unit) {
    LaunchedEffect(loaded) { onLoadMore() }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

/**
 * History of saved labels, sectioned by week and then by day: a photo grid or a list with time,
 * meal and dish. [records] is the part read so far; scrolling to its end asks for more.
 */
@Composable
fun Gallery(
    records: List<LabelRecord>,
    grid: Boolean,
    mealMapping: String,
    /** Paths of the selected records' TOML files; non-empty means selection mode. */
    selected: Set<String>,
    /** True while older entries exist that [records] does not hold yet. */
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onClick: (LabelRecord) -> Unit,
    onLongClick: (LabelRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(records) {
        History.byWeek(records) { it.taken.toLocalDate() }
            .map { (weekStart, items) -> weekStart to History.byDay(items) { it.taken.toLocalDate() } }
    }
    if (records.isEmpty()) {
        Text("Brak zapisanych etykiet.", style = MaterialTheme.typography.bodyMedium, modifier = modifier.padding(top = 12.dp))
        return
    }
    if (grid) LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        weeks.forEach { (weekStart, days) ->
            item(key = "w$weekStart", span = { GridItemSpan(maxLineSpan) }) { WeekHeader(weekStart) }
            days.forEach { (day, items) ->
                item(key = "d$day", span = { GridItemSpan(maxLineSpan) }) { DayHeader(day) }
                items(items, key = { it.toml.path }) { record ->
                    val isSelected = record.toml.path in selected
                    Box(Modifier.aspectRatio(1f).combinedClickable(onLongClick = { onLongClick(record) }) { onClick(record) }) {
                        Thumbnail(record.photo, Modifier.matchParentSize())
                        if (isSelected) Box(
                            Modifier.matchParentSize().clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        )
                        if (selected.isNotEmpty()) SelectionMark(isSelected, Modifier.align(Alignment.TopEnd).padding(6.dp))
                    }
                }
            }
        }
        if (hasMore) item(key = "more", span = { GridItemSpan(maxLineSpan) }) { LoadMore(records.size, onLoadMore) }
    } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        weeks.forEach { (weekStart, days) ->
            item(key = "w$weekStart") { WeekHeader(weekStart) }
            days.forEach { (day, items) ->
                item(key = "d$day") { DayHeader(day) }
                items(items, key = { it.toml.path }) { record ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (record.toml.path in selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .combinedClickable(onLongClick = { onLongClick(record) }) { onClick(record) }
                            .padding(vertical = 2.dp),
                    ) {
                        Thumbnail(record.photo, Modifier.size(64.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                listOfNotNull(record.taken.format(rowTime), record.mealLabel(mealMapping)).joinToString(" / "),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(record.dishName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (selected.isNotEmpty()) SelectionMark(record.toml.path in selected, Modifier.padding(end = 6.dp))
                    }
                }
            }
        }
        if (hasMore) item(key = "more") { LoadMore(records.size, onLoadMore) }
    }
}

// --------------------------------------------------------------- details screen

private val sectionTitles = listOf(
    LabelRecord.SAVED to "Zapisane (po poprawkach)",
    LabelRecord.DETECTED to "Odczytane z etykiety",
    LabelRecord.META to "Informacje",
)

private val keyLabels = mapOf(
    "name" to "Nazwa", "dish_name" to "Danie", "code" to "Kod", "day" to "Dzień na etykiecie",
    "kcal" to "kcal", "fat_g" to "Tłuszcz (g)", "carbs_g" to "Węglowodany (g)", "protein_g" to "Białko (g)",
    "meal_label" to "Posiłek", "date" to "Data posiłku", "sent_to" to "Wysłano do", "saved_at" to "Zapisano",
    "aaps_carbs_g" to "Węglowodany do AAPS (g)", "taken" to "Zdjęcie z", "model" to "Model",
    "provider" to "Dostawca modelu", "catering" to "Catering",
    "input_tokens" to "Tokeny wejściowe", "output_tokens" to "Tokeny wyjściowe", "cost_usd" to "Koszt odczytu",
    "photo" to "Plik", "error" to "Błąd",
)
private val hiddenKeys = setOf("meal", "cost_estimated")

/** A cost in USD with up to six decimals; an estimate from the built-in price list is marked as one. */
fun formatCost(usd: Double, estimated: Boolean): String {
    val rounded = BigDecimal(usd).setScale(6, RoundingMode.HALF_UP)
    val amount = when {
        rounded.signum() == 0 && usd > 0 -> "< 0,000001"
        rounded.signum() == 0 -> "0"
        else -> rounded.stripTrailingZeros().toPlainString().replace('.', ',')
    }
    return if (estimated) "≈ $amount USD (szacunek)" else "$amount USD"
}

/** Rows shown for one TOML section: (label, value), numbers written with a decimal comma. */
fun detailRows(section: Map<String, Any>): List<Pair<String, String>> =
    section.filterKeys { it !in hiddenKeys }.map { (key, value) ->
        (keyLabels[key] ?: key) to when {
            key == "cost_usd" && value is Number -> formatCost(value.toDouble(), section["cost_estimated"] == true)
            value is Double -> value.toString().removeSuffix(".0").replace('.', ',')
            else -> value.toString().replace('T', ' ')
        }
    }

/** The body of the details screen: the photo (tap to zoom) and every stored section of the entry. */
@Composable
fun RecordDetails(record: LabelRecord) {
    var zoom by remember { mutableStateOf(false) }
    if (zoom) ZoomablePhotoDialog(record.photo) { zoom = false }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Thumbnail(record.photo, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clickable { zoom = true }, maxEdge = 1280)
        Text(record.taken.format(fullDate), style = MaterialTheme.typography.labelLarge)
        sectionTitles.forEach { (name, title) ->
            val rows = detailRows(record.section(name))
            if (rows.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(top = 6.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                rows.forEach { (label, value) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.4f))
                        Text(value, modifier = Modifier.weight(0.6f))
                    }
                }
            }
        }
    }
}
