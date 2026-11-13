package pl.lovefitatu.bridge.automation

import android.content.Context
import android.content.pm.PackageManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The apps the accessibility service steers. [minVersion] is the oldest version it may steer. */
enum class SteeredApp(val label: String, val storageKey: String, val minVersion: String?) {
    FITATU("Fitatu", "version_check_fitatu", "4.17.0"),
    AAPS("AAPS", "version_check_aaps", "4.0.0"),
}

/**
 * Result of one version check. [version] is null when the package is not installed; [updatedAt] is
 * when the system last installed or updated the package, [at] when the check ran.
 */
data class VersionCheck(val packageName: String, val version: String?, val updatedAt: Long, val ok: Boolean, val at: Long) {

    /** True when [other] found the same installed package and reached the same verdict. */
    fun sameFinding(other: VersionCheck): Boolean = copy(at = other.at) == other

    fun describe(app: SteeredApp): String = when {
        version == null -> "${app.label} nie jest zainstalowane (pakiet $packageName)"
        ok -> "${app.label} $version"
        else -> "${app.label} ${version.ifBlank { "w nieznanej wersji" }} jest za stare, wymagana wersja ${app.minVersion} lub nowsza"
    }

    /** Short form of [describe] for a button. */
    fun summary(app: SteeredApp): String = when {
        version == null -> "${app.label}: brak aplikacji"
        ok -> "${app.label} $version"
        else -> "${app.label} ${version.ifBlank { "?" }}: za stara wersja"
    }

    fun checkedAt(zone: ZoneId = ZoneId.systemDefault()): String = TIME.format(Instant.ofEpochMilli(at).atZone(zone))

    fun encode(): String = listOf(packageName, version ?: MISSING, updatedAt, ok, at).joinToString("\t")

    companion object {
        private const val MISSING = "<none>"
        private val TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        fun decode(text: String?): VersionCheck? {
            val parts = text?.split('\t')?.takeIf { it.size == 5 } ?: return null
            return VersionCheck(
                parts[0], parts[1].takeIf { it != MISSING }, parts[2].toLongOrNull() ?: return null,
                parts[3] == "true", parts[4].toLongOrNull() ?: return null,
            )
        }
    }
}

object AppVersions {
    /** Compares the leading numeric parts, so "4.0.0-dev" and "4.0" both count as 4.0.0. */
    fun atLeast(version: String, min: String): Boolean {
        val have = numbers(version)
        val want = numbers(min)
        if (have.isEmpty()) return false
        for (i in 0 until maxOf(have.size, want.size)) {
            val diff = have.getOrElse(i) { 0 }.compareTo(want.getOrElse(i) { 0 })
            if (diff != 0) return diff > 0
        }
        return true
    }

    private fun numbers(version: String): List<Int> =
        version.trim().split('.').map { part -> part.takeWhile(Char::isDigit) }
            .takeWhile { it.isNotEmpty() }.mapNotNull(String::toIntOrNull)

    fun evaluate(app: SteeredApp, packageName: String, version: String?, updatedAt: Long, at: Long): VersionCheck {
        val ok = version != null && (app.minVersion == null || atLeast(version, app.minVersion))
        return VersionCheck(packageName, version, updatedAt, ok, at)
    }

    /** Reads the installed version from the package manager; nothing is opened or steered. */
    fun check(context: Context, app: SteeredApp, packageName: String): VersionCheck {
        val info = try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return evaluate(app, packageName, info?.let { it.versionName.orEmpty() }, info?.lastUpdateTime ?: 0, System.currentTimeMillis())
    }
}
