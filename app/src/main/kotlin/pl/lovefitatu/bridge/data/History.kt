package pl.lovefitatu.bridge.data

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * One saved label: `<root>/YYYY-MM-DD/HH-mm-ss.jpg` plus a `.toml` beside it with the sections
 * `[meta]`, `[detected]` (what the model read) and `[saved]` (what the user sent on, after edits).
 */
class LabelRecord(val toml: File, val data: TomlDoc) {
    val photo: File = File(toml.parentFile, toml.nameWithoutExtension + ".jpg")

    /** From the folder and file name, so it survives a damaged TOML. */
    val taken: LocalDateTime = runCatching {
        LocalDateTime.of(
            LocalDate.parse(toml.parentFile!!.name),
            LocalTime.parse(toml.nameWithoutExtension.take(8), History.TIME_FILE),
        )
    }.getOrElse { LocalDateTime.MIN }

    fun section(name: String): Map<String, Any> = data[name].orEmpty()
    fun text(section: String, key: String): String? = data[section]?.get(key)?.toString()?.takeIf { it.isNotBlank() }

    val dishName: String
        get() = text(SAVED, "name") ?: listOfNotNull(text(DETECTED, "code"), text(DETECTED, "dish_name"))
            .joinToString(" ").ifBlank { text(META, "error")?.let { "Błąd odczytu" } ?: "Nie odczytano" }

    fun mealLabel(mealMapping: String): String? =
        text(SAVED, "meal_label") ?: MealMapping.mealFor(DishCode.parse(text(DETECTED, "code"))?.mealLetter, mealMapping)?.label

    companion object {
        const val META = "meta"
        const val DETECTED = "detected"
        const val SAVED = "saved"
    }
}

object History {
    val TIME_FILE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss")

    /** App-specific external folder (readable over USB/adb), internal storage if that is unavailable. */
    fun root(context: Context): File = (context.getExternalFilesDir(null) ?: context.filesDir).resolve("labels")

    /** Saves the photo and a TOML holding only `[meta]`; detection and saved values are added later. */
    fun create(context: Context, photo: Bitmap, taken: LocalDateTime, model: String, provider: String, catering: String): LabelRecord {
        val dir = root(context).resolve(taken.toLocalDate().toString()).apply { mkdirs() }
        var stem = taken.format(TIME_FILE)
        var n = 2
        while (File(dir, "$stem.toml").exists() || File(dir, "$stem.jpg").exists()) stem = "${taken.format(TIME_FILE)}-${n++}"

        File(dir, "$stem.jpg").outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val meta = mapOf(
            "taken" to taken.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "photo" to "$stem.jpg",
            "model" to model,
            "provider" to provider,
            "catering" to catering,
        )
        return write(File(dir, "$stem.toml"), mapOf(LabelRecord.META to meta))
    }

    /** Replaces (or, with [merge], adds to) one section and rewrites the file. Returns the new record. */
    fun update(record: LabelRecord, section: String, values: Map<String, Any>, merge: Boolean = false): LabelRecord {
        val data = LinkedHashMap(record.data)
        data[section] = if (merge) record.section(section) + values else values
        // Keep a stable, readable order whatever order sections were written in.
        val ordered = listOf(LabelRecord.META, LabelRecord.DETECTED, LabelRecord.SAVED)
            .mapNotNull { name -> data[name]?.let { name to it } }.toMap() + data
        return write(record.toml, ordered)
    }

    private fun write(file: File, data: TomlDoc): LabelRecord {
        file.writeText(Toml.write(data))
        return LabelRecord(file, data)
    }

    /** All records, newest first. */
    fun list(context: Context): List<LabelRecord> =
        root(context).listFiles { f -> f.isDirectory }.orEmpty().flatMap { dir ->
            dir.listFiles { f -> f.extension == "toml" }.orEmpty().map { file ->
                LabelRecord(file, runCatching { Toml.parse(file.readText()) }.getOrDefault(emptyMap()))
            }
        }.sortedByDescending { it.taken }

    /** Deletes the photo and TOML of each record, and the day folder once it is empty. */
    fun delete(records: Collection<LabelRecord>) {
        for (record in records) {
            record.photo.delete()
            record.toml.delete()
            record.toml.parentFile?.takeIf { it.list()?.isEmpty() == true }?.delete()
        }
    }

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Groups newest-first records into Monday-based weeks, newest week first. */
    fun <T> byWeek(items: List<T>, date: (T) -> LocalDate): List<Pair<LocalDate, List<T>>> =
        items.groupBy { weekStart(date(it)) }.toList().sortedByDescending { it.first }
}
