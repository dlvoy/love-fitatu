package pl.lovefitatu.bridge.automation

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import pl.lovefitatu.bridge.data.Settings
import java.time.format.DateTimeFormatter

/**
 * Drives Fitatu and AAPS through their accessibility trees. Idle unless the bridge app starts a job
 * with [start]; the service reacts to no events on its own.
 */
class BridgeAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running: Job? = null

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    /** Runs [jobs] in order, stopping at the first failure. Returns false if a run is already active. */
    fun start(jobs: List<AutomationJob>): Boolean {
        if (running?.isActive == true) return false
        running = scope.launch {
            for (job in jobs) {
                val result = try {
                    withTimeout(45_000) {
                        when (job) {
                            is AutomationJob.Fitatu -> runFitatu(job)
                            is AutomationJob.Aaps -> runAaps(job)
                        }
                    }
                } catch (e: StepFailed) {
                    report("Przerwano: ${e.message}" + hint(job))
                    return@launch
                } catch (e: TimeoutCancellationException) {
                    report("Przerwano: przekroczono czas" + hint(job))
                    return@launch
                }
                report(result)
            }
        }
        return true
    }

    private fun hint(job: AutomationJob) =
        if (job is AutomationJob.Aaps) " (węglowodany do wpisania ręcznie: ${job.carbs} g)" else ""

    private fun report(message: String) {
        Log.i(TAG, message)
        AutomationStatus.message.value = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ---------------------------------------------------------------- Fitatu

    private suspend fun runFitatu(job: AutomationJob.Fitatu): String {
        val pkg = Settings.FITATU_PACKAGE
        launchApp(pkg)

        // 1. Get to the day planner, backing out of the search / quick-add screens if Fitatu was left there.
        val deadline = SystemClock.uptimeMillis() + 20_000
        while (true) {
            val root = root(pkg)
            if (root?.byId(F_TODAY) != null) break
            if (SystemClock.uptimeMillis() > deadline) fail("nie znaleziono ekranu Jadłospis w Fitatu")
            if (root != null && (root.byId(F_FORM_BACK) != null || root.byId(F_SEARCH_BACK) != null)) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(900)
            } else delay(250)
        }

        // 2. Select the day.
        val dayId = "planner_page_planner_week_app_bar_${job.date.format(DAY_ID)}_button"
        val day = waitOrNull(pkg, 3_000) { it.byId(dayId) }
            ?: fail("dzień ${job.date} nie jest widoczny w bieżącym tygodniu Fitatu")
        // Fitatu marks the shown day as selected. Leave it alone when it is already the right one
        // (tapping the selected day again is not a no-op in Fitatu), otherwise tap and confirm.
        if (!day.isSelected) {
            click(day, "dzień")
            waitOrNull(pkg, 4_000) { r -> r.byId(dayId)?.takeIf { it.isSelected } }
                ?: fail("Fitatu nie przełączyło się na dzień ${job.date}")
            delay(600)
        }

        // 3. Find the meal's add button, scrolling the meal list if needed.
        val addId = "planner_page_planner_${job.meal.fitatuKey}_section_add_button"
        var add: AccessibilityNodeInfo? = null
        for (attempt in 0 until 8) {
            add = waitOrNull(pkg, 1_200) { it.byId(addId) }
            if (add != null) break
            // The planner has three scrollables: the week bar, the day pager and the meal list.
            // Only the meal list may be scrolled; scrolling the others changes the week or the day.
            val mealList = root(pkg)?.let(::mealList) ?: fail("nie znaleziono listy posiłków w Fitatu")
            if (!mealList.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) break
            delay(700)
        }
        // Scrolling must not have moved the planner to another day.
        if (root(pkg)?.byId(dayId)?.isSelected != true) fail("Fitatu zmieniło dzień podczas przewijania, przerwano")
        click(add ?: fail("nie znaleziono posiłku ${job.meal.label} w Fitatu"), "dodaj do ${job.meal.label}")

        // 4. Confirm the search screen is for the right meal before touching anything else.
        val selector = waitFor(pkg, "ekran wyszukiwania Fitatu") { it.byId(F_MEAL_SELECTOR) }
        val shown = selector.label().trim()
        if (!shown.equals(job.meal.label, ignoreCase = true)) fail("Fitatu otworzyło posiłek „$shown” zamiast „${job.meal.label}”")

        // 5. Open quick add.
        click(waitFor(pkg, "przycisk Szybkie dodawanie") { it.byId(F_QUICK_ADD) }, "Szybkie dodawanie")
        waitFor(pkg, "formularz Szybkie dodawanie") { it.byId(F_NAME) }

        // 6. Fill and read back.
        setField(pkg, F_NAME, job.name, exact = true)
        setField(pkg, F_KCAL, Format.fitatuNumber(job.kcal))
        setField(pkg, F_FAT, Format.fitatuNumber(job.fat))
        setField(pkg, F_CARBS, Format.fitatuNumber(job.carbs))
        setField(pkg, F_PROTEIN, Format.fitatuNumber(job.protein))

        if (!job.autoSave) return "Fitatu: formularz wypełniony, sprawdź i naciśnij Zapisz"

        // 7. Save and wait for the form to close.
        click(waitFor(pkg, "przycisk Zapisz") { it.byId(F_SAVE) }, "Zapisz")
        waitOrNull(pkg, 8_000) { r -> r.takeIf { it.byId(F_NAME) == null } }
            ?: fail("Fitatu nie zamknęło formularza po Zapisz, sprawdź wpis")
        return "Fitatu: dodano „${job.name.take(30)}” do ${job.meal.label}"
    }

    private suspend fun setField(pkg: String, id: String, value: String, exact: Boolean = false) {
        val holder = waitFor(pkg, "pole $id") { it.byId(id) }
        // Flutter puts the real text field one level below the node that carries the id.
        val target = holder.walk().lastOrNull { it !== holder && it.className == EDIT_TEXT } ?: holder

        // ACTION_SET_TEXT is useless here: Fitatu (Flutter) then reports the text to accessibility
        // but its form stays empty. So focus the field with a tap and type through the input
        // connection, exactly as a keyboard would.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) fail("wpisywanie do Fitatu wymaga Androida 13 lub nowszego")
        if (!target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) fail("nie można wybrać pola $id")
        waitOrNull(pkg, 2_500) { r -> r.byId(id)?.walk()?.firstOrNull { it.isFocused } }
        delay(400) // lets Fitatu rebind the input connection to this field

        val matches: (String?) -> Boolean = { if (exact) it?.trim() == value.trim() else Format.sameNumber(it, value) }
        val connection = withTimeoutOrNull(3_000) {
            var c = inputMethod?.currentInputConnection
            while (c == null) {
                delay(150)
                c = inputMethod?.currentInputConnection
            }
            c
        } ?: fail("Fitatu nie otworzyło pola $id do edycji")

        // Clear whatever is there, then type.
        connection.getSurroundingText(4_000, 4_000, 0)?.let { around ->
            connection.deleteSurroundingText(around.selectionStart, around.text.length - around.selectionEnd)
        }
        // Fitatu's number fields take a decimal point from the keyboard and show it as a comma.
        connection.commitText(if (exact) value else value.replace(',', '.'), 1, null)

        // Verify against the editor's own content, which (unlike the accessibility text) is the form's real state.
        withTimeoutOrNull(2_500) {
            while (!matches(inputMethod?.currentInputConnection?.getSurroundingText(4_000, 4_000, 0)?.text?.toString())) delay(150)
            true
        } ?: fail("pole $id nie przyjęło wartości „$value”")
    }

    // ------------------------------------------------------------------ AAPS

    /**
     * Opens the bolus calculator and types the carbs. The only actions performed inside AAPS are one
     * click on the "Kalkulator bolusa" button and one set-text on the carbs field. Nothing after that.
     */
    private suspend fun runAaps(job: AutomationJob.Aaps): String {
        val pkg = job.packageName
        if (!AapsGuard.validCarbs(job.carbs)) fail("węglowodany ${job.carbs} g poza zakresem 1–${AapsGuard.MAX_CARBS}")
        launchApp(pkg)

        // 1. Main screen only. No navigation inside AAPS: if the button is not there, give up.
        val button = waitOrNull(pkg, 8_000) { r ->
            r.walk().firstOrNull { AapsGuard.isCalculatorLabel(it.contentDescription) }
        } ?: fail("AAPS nie jest na ekranie głównym (brak przycisku Kalkulator bolusa)")

        // 2. The one allowed click.
        val clickable = generateSequence(button) { it.parent }.take(3).firstOrNull { it.isClickable }
            ?: fail("przycisk Kalkulator bolusa nie jest klikalny")
        clickInAaps(clickable)

        // 3. Calculator screen: title text plus exactly one text field, labelled as carbs.
        val field = waitOrNull(pkg, 8_000) { r ->
            val nodes = r.walk().toList()
            val titled = nodes.any { AapsGuard.isCalculatorLabel(it.text) }
            val carbsLabelled = nodes.any { AapsGuard.isCarbsLabel(it.text) }
            val edits = nodes.filter { it.className == EDIT_TEXT }
            edits.singleOrNull()?.takeIf { titled && carbsLabelled }
        } ?: fail("nie rozpoznano ekranu kalkulatora bolusa w AAPS")

        // 4. The one allowed set-text, then read back.
        val text = job.carbs.toString()
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) fail("nie można wpisać węglowodanów w AAPS")
        waitOrNull(pkg, 3_000) { r ->
            r.walk().firstOrNull { it.className == EDIT_TEXT && it.text?.toString()?.trim() == text }
        } ?: fail("AAPS nie przyjął wartości węglowodanów, sprawdź pole")

        // 5. Stop. Confirming is the user's decision and the user's tap.
        return "AAPS: wpisano ${job.carbs} g. Sprawdź kalkulator i zatwierdź samodzielnie."
    }

    /** Click used inside AAPS: refuses any node whose subtree carries a confirm-like label. */
    private fun clickInAaps(node: AccessibilityNodeInfo) {
        val labels = node.walk().flatMap { sequenceOf(it.text, it.contentDescription) }.filterNotNull().toList()
        if (labels.any { AapsGuard.isForbiddenLabel(it) }) fail("odmowa kliknięcia elementu zatwierdzającego w AAPS")
        if (labels.none { AapsGuard.isCalculatorLabel(it) }) fail("odmowa kliknięcia nieznanego elementu w AAPS")
        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) fail("kliknięcie Kalkulator bolusa nie powiodło się")
    }

    // --------------------------------------------------------------- helpers

    private fun launchApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: fail("aplikacja $pkg nie jest zainstalowana")
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun root(pkg: String): AccessibilityNodeInfo? =
        rootInActiveWindow?.takeIf { it.packageName?.toString() == pkg }

    private suspend fun <T : Any> waitOrNull(pkg: String, timeoutMs: Long, find: (AccessibilityNodeInfo) -> T?): T? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            root(pkg)?.let(find)?.let { return it }
            if (SystemClock.uptimeMillis() > deadline) return null
            delay(200)
        }
    }

    private suspend fun <T : Any> waitFor(pkg: String, what: String, find: (AccessibilityNodeInfo) -> T?): T =
        waitOrNull(pkg, 7_000, find) ?: fail("nie znaleziono: $what")

    /** Click for Fitatu nodes. AAPS clicks go through [clickInAaps] only. */
    private fun click(node: AccessibilityNodeInfo, what: String) {
        check(node.packageName?.toString() == Settings.FITATU_PACKAGE) { "click() is for Fitatu only" }
        val target = generateSequence(node) { it.parent }.take(3).firstOrNull { it.isClickable } ?: node
        if (!target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) fail("kliknięcie „$what” nie powiodło się")
    }

    /** The vertically scrolling meal list: the nearest scrollable ancestor of any meal section node. */
    private fun mealList(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val anchor = root.walk().firstOrNull { it.viewIdResourceName?.contains(F_SECTION_MARKER) == true } ?: return null
        return generateSequence(anchor.parent) { it.parent }.firstOrNull { it.isScrollable }
    }

    private fun fail(message: String): Nothing = throw StepFailed(message)

    companion object {
        private const val TAG = "LoveFitatuBridge"
        private const val EDIT_TEXT = "android.widget.EditText"
        private val DAY_ID = DateTimeFormatter.ofPattern("yyyy_MM_dd")

        private const val F_TODAY = "planner_page_planner_today_button"
        private const val F_SECTION_MARKER = "_section_add_button"
        private const val F_SEARCH_BACK = "search_catalog_page_app_bar_back_button"
        private const val F_MEAL_SELECTOR = "search_catalog_page_app_bar_meal_selector_dropdown"
        private const val F_QUICK_ADD = "search_catalog_page_search_body_search_bottom_bar_add_quick_button"
        private const val F_FORM_BACK = "quick_food_form_page_app_bar_back_button"
        private const val F_SAVE = "quick_food_form_page_app_bar_save_button"
        private const val F_NAME = "quick_food_form_page_name_text_field"
        private const val F_KCAL = "quick_food_form_page_energy_value_text_field"
        private const val F_FAT = "quick_food_form_page_fat_text_field"
        private const val F_CARBS = "quick_food_form_page_carbohydrates_text_field"
        private const val F_PROTEIN = "quick_food_form_page_protein_text_field"

        /** Set while the service is enabled in system accessibility settings. */
        @Volatile
        var instance: BridgeAccessibilityService? = null
            private set
    }
}

// Node helpers -------------------------------------------------------------

fun AccessibilityNodeInfo.walk(): Sequence<AccessibilityNodeInfo> = sequence {
    val stack = ArrayDeque<AccessibilityNodeInfo>()
    stack.addLast(this@walk)
    var visited = 0
    while (stack.isNotEmpty() && visited++ < 4000) {
        val node = stack.removeLast()
        yield(node)
        for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
    }
}

/** Matches Flutter's bare ids as well as fully qualified `package:id/name`. */
fun AccessibilityNodeInfo.byId(id: String): AccessibilityNodeInfo? = walk().firstOrNull {
    val rid = it.viewIdResourceName
    rid != null && (rid == id || rid.endsWith("/$id"))
}

fun AccessibilityNodeInfo.label(): String = (text ?: contentDescription ?: "").toString()
