package pl.cukrzycowy.labelbite

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.cukrzycowy.labelbite.automation.AutomationJob
import pl.cukrzycowy.labelbite.automation.AutomationStatus
import pl.cukrzycowy.labelbite.automation.BridgeAccessibilityService
import pl.cukrzycowy.labelbite.automation.SteeredApp
import pl.cukrzycowy.labelbite.data.History
import pl.cukrzycowy.labelbite.data.LabelRecord
import pl.cukrzycowy.labelbite.data.SecureStore
import pl.cukrzycowy.labelbite.data.Settings
import pl.cukrzycowy.labelbite.ocr.ImagePrep
import pl.cukrzycowy.labelbite.ocr.LabelReader
import pl.cukrzycowy.labelbite.ocr.LabelResult
import pl.cukrzycowy.labelbite.ocr.LlmException
import pl.cukrzycowy.labelbite.ui.AccessibilityButton
import pl.cukrzycowy.labelbite.ui.AppLogo
import pl.cukrzycowy.labelbite.ui.Gallery
import pl.cukrzycowy.labelbite.ui.InfoNote
import pl.cukrzycowy.labelbite.ui.LabelBiteTheme
import pl.cukrzycowy.labelbite.ui.RecordDetails
import pl.cukrzycowy.labelbite.ui.ReviewData
import pl.cukrzycowy.labelbite.ui.ReviewScreen
import pl.cukrzycowy.labelbite.ui.SettingsScreen
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    private enum class Screen { MAIN, DETAILS, REVIEW, SETTINGS }

    private lateinit var settings: Settings
    private var unlocked by mutableStateOf(false)
    private var lockMessage by mutableStateOf<String?>(null)
    private var autoPrompted = false
    private var pendingIntent: Intent? = null

    private var screen by mutableStateOf(Screen.MAIN)
    private var review by mutableStateOf<ReviewData?>(null)
    private var busy by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)
    private var serviceEnabled by mutableStateOf(false)
    /** All-files access, which puts the history in the shared Documents folder. */
    private var storageGranted by mutableStateOf(false)
    /** The newest history entries read so far; the gallery asks for more as it is scrolled. */
    private var records by mutableStateOf(emptyList<LabelRecord>())

    /** TOML files of every history entry, newest first. Entries past [records] are not read yet. */
    private var historyFiles by mutableStateOf(emptyList<File>())
    private val historyLock = Mutex()
    private var galleryGrid by mutableStateOf(true)
    /** The history entry shown on the details screen. */
    private var opened by mutableStateOf<LabelRecord?>(null)

    /** TOML paths of history entries ticked in selection mode (entered by long press). */
    private var selected by mutableStateOf(emptySet<String>())
    private var confirmDelete by mutableStateOf(false)
    private var photoUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) photoUri?.let(::process)
    }
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::process)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = Settings(this)
        pendingIntent = intent
        storageGranted = Environment.isExternalStorageManager()

        // The store stays unlocked for the life of the process, so a recreated activity skips the prompt.
        if (!SecureStore.unlocked && !SecureStore.isBiometricMode(this)) SecureStore.unlockPlain(this)
        if (SecureStore.unlocked) onUnlocked()

        setContent {
            LabelBiteTheme {
                if (!unlocked) {
                    LockScreen()
                    return@LabelBiteTheme
                }
                val status by AutomationStatus.message.collectAsState()
                BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }
                val selecting = screen == Screen.MAIN && selected.isNotEmpty()
                BackHandler(enabled = selecting) { selected = emptySet() }
                if (confirmDelete) AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Usunąć zaznaczone?") },
                    text = { Text("Zdjęcia i zapisane dane (${selected.size}) zostaną trwale usunięte z historii aplikacji.") },
                    confirmButton = { TextButton(::deleteSelected) { Text("Usuń", color = MaterialTheme.colorScheme.error) } },
                    dismissButton = { TextButton({ confirmDelete = false }) { Text("Anuluj") } },
                )
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (screen == Screen.MAIN && !selecting) AppLogo(36.dp)
                                    Text(
                                        when {
                                            selecting -> "Zaznaczono: ${selected.size}"
                                            screen == Screen.SETTINGS -> "Ustawienia"
                                            screen == Screen.DETAILS -> "Szczegóły etykiety"
                                            screen == Screen.REVIEW -> "Edycja etykiety"
                                            else -> "LabelBite"
                                        }
                                    )
                                }
                            },
                            navigationIcon = {
                                if (screen != Screen.MAIN) IconButton({ screen = Screen.MAIN }) {
                                    Icon(painterResource(R.drawable.ic_arrow_back), "Wróć")
                                }
                            },
                            actions = {
                                if (selecting) {
                                    IconButton({ confirmDelete = true }) {
                                        Icon(painterResource(R.drawable.ic_delete), "Usuń zaznaczone")
                                    }
                                    TextButton({ selected = emptySet() }) { Text("Anuluj") }
                                } else if (screen == Screen.MAIN) IconButton({
                                    galleryGrid = !galleryGrid
                                    settings.galleryGrid = galleryGrid
                                }) {
                                    // Shows the view you would switch to.
                                    if (galleryGrid) Icon(painterResource(R.drawable.ic_list), "Pokaż jako listę")
                                    else Icon(painterResource(R.drawable.ic_grid), "Pokaż jako siatkę zdjęć")
                                }
                                if (!selecting && screen == Screen.MAIN) IconButton({ screen = Screen.SETTINGS }) {
                                    Icon(painterResource(R.drawable.ic_settings), "Ustawienia")
                                }
                                if (screen == Screen.DETAILS) opened?.let { record ->
                                    IconButton({ reopen(record) }) {
                                        Icon(painterResource(R.drawable.ic_edit), "Otwórz do edycji")
                                    }
                                }
                            },
                        )
                    },
                ) { padding ->
                    val base = Modifier.fillMaxSize().padding(padding).imePadding()
                    if (screen == Screen.MAIN) Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = base.padding(horizontal = 16.dp),
                    ) {
                        MainContent(status)
                        Notices()
                        Gallery(
                            records, galleryGrid, settings.mealMapping, selected,
                            hasMore = records.size < historyFiles.size,
                            onLoadMore = ::loadMoreHistory,
                            onClick = {
                                if (selected.isNotEmpty()) toggleSelected(it)
                                else {
                                    opened = it
                                    screen = Screen.DETAILS
                                }
                            },
                            onLongClick = ::toggleSelected,
                            modifier = Modifier.weight(1f),
                        )
                    } else Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = base.verticalScroll(rememberScrollState()).padding(16.dp),
                    ) {
                        when (screen) {
                            Screen.SETTINGS -> SettingsScreen(
                                settings, serviceEnabled, storageGranted,
                                ::openAccessibilitySettings, ::openStorageAccessSettings, ::openUrl,
                            )
                            Screen.DETAILS -> opened?.let { RecordDetails(it) }
                            else -> review?.let { ReviewScreen(it, settings, status, ::run) }
                        }
                        Notices()
                    }
                }
            }
        }
    }

    @Composable
    private fun LockScreen() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp, vertical = 16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                // A phone held sideways has little height, so the logo shrinks to leave room for the button.
                AppLogo(if (LocalConfiguration.current.screenHeightDp < 520) 96.dp else 176.dp)
                Text("LabelBite", style = MaterialTheme.typography.headlineSmall)
                lockMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                Button(::promptUnlock) {
                    Icon(painterResource(R.drawable.ic_fingerprint), null, modifier = Modifier.size(20.dp))
                    Text("Odblokuj", modifier = Modifier.padding(start = 8.dp))
                }
            }
            InfoNote("Konfiguracja jest zabezpieczona biometrią.")
        }
    }

    @Composable
    private fun MainContent(status: String?) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(::launchCamera, enabled = !busy, shape = captureShape, modifier = Modifier.weight(1f)) {
                CaptureLabel(R.drawable.ic_photo_camera, "Zrób zdjęcie")
            }
            OutlinedButton(
                { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !busy,
                shape = captureShape,
                modifier = Modifier.weight(1f),
            ) { CaptureLabel(R.drawable.ic_photo_library, "Z galerii") }
        }
        if (busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Text("Odczytywanie etykiety…")
        }
        if (review != null && !busy) OutlinedButton({ screen = Screen.REVIEW }, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_history), null, modifier = Modifier.size(18.dp))
            Text("Wróć do ostatniej etykiety", modifier = Modifier.padding(start = 8.dp))
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    /** The content of a capture button: a large icon centred above its label. */
    @Composable
    private fun CaptureLabel(icon: Int, label: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Icon(painterResource(icon), null, modifier = Modifier.size(40.dp))
            Text(label)
        }
    }

    @Composable
    private fun Notices() {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val steering = screen == Screen.MAIN || screen == Screen.REVIEW
        if (!serviceEnabled && steering) AccessibilityButton(false, ::openAccessibilitySettings)
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = BridgeAccessibilityService.instance != null
        // Granted or taken away on a system screen, so it is looked at on every return to the app.
        val granted = Environment.isExternalStorageManager()
        if (granted != storageGranted) {
            storageGranted = granted
            if (unlocked) reloadHistory()
        }
        if (!unlocked && !autoPrompted) {
            autoPrompted = true
            promptUnlock()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (unlocked) handleIntent(intent) else pendingIntent = intent
    }

    // ------------------------------------------------------------ unlocking

    private fun promptUnlock() {
        lockMessage = null
        val cipher = try {
            SecureStore.unlockCipher()
        } catch (e: Exception) {
            // Typically a new fingerprint was enrolled, which permanently invalidates the key.
            SecureStore.reset(this)
            SecureStore.unlockPlain(this)
            onUnlocked()
            error = "Zabezpieczenia biometryczne urządzenia zmieniły się, więc zapisana konfiguracja " +
                "(w tym klucz API) została usunięta. Wprowadź ją ponownie w Ustawieniach."
            return
        }
        BiometricPrompt.Builder(this)
            .setTitle("LabelBite")
            .setSubtitle("Odblokuj konfigurację i klucz API")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButton("Anuluj", mainExecutor) { _, _ -> }
            .build()
            .authenticate(
                BiometricPrompt.CryptoObject(cipher), CancellationSignal(), mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            SecureStore.finishUnlock(this@MainActivity, result.cryptoObject!!.cipher!!)
                            onUnlocked()
                        } catch (e: Exception) {
                            lockMessage = "Nie udało się odszyfrować konfiguracji: ${e.message}"
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        lockMessage = errString.toString()
                    }
                },
            )
    }

    private fun onUnlocked() {
        unlocked = true
        galleryGrid = settings.galleryGrid
        reloadHistory()
        pendingIntent?.let(::handleIntent)
        pendingIntent = null
    }

    // -------------------------------------------------------------- intents

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let(::process)
        }
        // Debug builds only: open the review screen with preset values, to test automation without the API.
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        intent.getStringExtra("debug_label")?.takeIf { debuggable }?.let {
            showReview(LabelResult.parse(it), null, null)
        }
    }

    private fun launchCamera() {
        val file = File(cacheDir, "photos").apply { mkdirs() }.resolve("label.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        photoUri = uri
        takePicture.launch(uri)
    }

    // ------------------------------------------------------- photo → review

    private fun process(uri: Uri) {
        error = null
        // Read once, so a settings change while the photo is processed cannot mix providers.
        val provider = settings.provider
        val apiKey = settings.apiKeyFor(provider)
        val model = settings.modelFor(provider)
        val catering = settings.catering
        val prompt = settings.labelPrompt
        if (apiKey.isBlank()) {
            error = "Najpierw ustaw klucz API ${provider.label}."
            screen = Screen.SETTINGS
            return
        }
        busy = true
        screen = Screen.MAIN
        val taken = LocalDateTime.now()
        lifecycleScope.launch {
            var record: LabelRecord? = null
            try {
                // The photo goes into history before the model is asked, so a failed read is kept too.
                val photo = withContext(Dispatchers.IO) { ImagePrep.load(this@MainActivity, uri, ImagePrep.STORED_EDGE) }
                record = withContext(Dispatchers.IO) { History.create(this@MainActivity, photo, taken, model, provider.id, catering.id) }
                reloadHistory()
                val dataUrl = withContext(Dispatchers.Default) {
                    ImagePrep.toDataUrl(ImagePrep.scaled(photo, ImagePrep.UPLOAD_EDGE))
                }
                val (result, usage) = LabelReader(provider, apiKey, model, prompt).readLabel(dataUrl)
                record = withContext(Dispatchers.IO) {
                    val billed = History.update(record!!, LabelRecord.META, usage?.toToml().orEmpty(), merge = true)
                    History.update(billed, LabelRecord.DETECTED, result.toToml())
                }
                showReview(result, photo, record)
            } catch (e: Exception) {
                val message = if (e is LlmException) e.message.orEmpty() else "Błąd: ${e.message ?: e.javaClass.simpleName}"
                error = message + ((e as? LlmException)?.rawReply?.let { "\n\n${it.take(600)}" } ?: "")
                // A reply the app could not use was still billed, so its usage is kept with the error.
                val meta = (e as? LlmException)?.usage?.toToml().orEmpty() + mapOf("error" to message)
                record?.let { r ->
                    withContext(Dispatchers.IO) { History.update(r, LabelRecord.META, meta, merge = true) }
                }
            } finally {
                busy = false
                reloadHistory()
            }
        }
    }

    private fun showReview(result: LabelResult, photo: Bitmap?, record: LabelRecord?) {
        AutomationStatus.message.value = null
        review = ReviewData(result, settings.mealMapping, LocalDate.now(), photo, record)
        screen = Screen.REVIEW
    }

    /** Opens a history entry in the review screen, with its saved values if it has any. */
    private fun reopen(record: LabelRecord) {
        lifecycleScope.launch {
            val photo = withContext(Dispatchers.IO) { ImagePrep.loadFile(record.photo, ImagePrep.STORED_EDGE) }
            showReview(LabelResult.fromToml(record.section(LabelRecord.DETECTED)), photo, record)
        }
    }

    private fun toggleSelected(record: LabelRecord) {
        val path = record.toml.path
        selected = if (path in selected) selected - path else selected + path
    }

    private fun deleteSelected() {
        val doomed = records.filter { it.toml.path in selected }
        confirmDelete = false
        selected = emptySet()
        // The open review may point at a deleted entry; detach it so a later send does not recreate the file.
        review?.let { data -> if (doomed.any { it.toml.path == data.record?.toml?.path }) data.record = null }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { History.delete(doomed) }
            reloadHistory()
        }
    }

    /**
     * Lists the history again and re-reads as many entries as were shown, at least one page. Entries
     * still in the app's own folder are first moved to the shared one, once all-files access allows it.
     */
    private fun reloadHistory() {
        lifecycleScope.launch {
            historyLock.withLock {
                val wanted = maxOf(HISTORY_PAGE, records.size)
                // Not while a label is being read: its entry is still written to under its old path.
                val migrate = !busy
                val (moved, files, loaded) = withContext(Dispatchers.IO) {
                    val moved = if (migrate) History.migrate(this@MainActivity) else 0
                    val files = History.files(this@MainActivity)
                    Triple(moved, files, History.load(files.take(wanted)))
                }
                if (moved > 0) {
                    // Whatever still points at a moved entry follows it to its new place.
                    selected = emptySet()
                    opened = opened?.let { History.relocated(this@MainActivity, it) }
                    review?.let { data -> data.record = data.record?.let { History.relocated(this@MainActivity, it) } }
                }
                historyFiles = files
                records = loaded
            }
        }
    }

    /** Reads the next page of older entries; called when the gallery is scrolled to its end. */
    private fun loadMoreHistory() {
        lifecycleScope.launch {
            historyLock.withLock {
                val next = historyFiles.drop(records.size).take(HISTORY_PAGE)
                if (next.isNotEmpty()) records = records + withContext(Dispatchers.IO) { History.load(next) }
            }
        }
    }

    // ----------------------------------------------------------- automation

    private fun run(jobs: List<AutomationJob>) {
        error = null
        val service = BridgeAccessibilityService.instance
        serviceEnabled = service != null
        val versionProblem = if (service == null) null else jobs.firstNotNullOfOrNull(::versionProblem)
        val started = versionProblem == null && service?.start(jobs) == true
        when {
            service == null -> error = "Usługa ułatwień dostępu jest wyłączona."
            versionProblem != null -> error = versionProblem
            !started -> error = "Poprzednia automatyzacja jeszcze trwa."
        }
        // Record what the user settled on, whether or not the hand-off could start.
        val data = review ?: return
        val record = data.record ?: return
        val sentTo = jobs.joinToString("+") { if (it is AutomationJob.Fitatu) "fitatu" else "aaps" }.takeIf { started }
        data.record = History.update(record, LabelRecord.SAVED, data.savedToml(sentTo))
        reloadHistory()
    }

    /**
     * Looks at the installed version of the app [job] is about to steer, refreshing the check on
     * record. Returns the reason the app must not be steered, or null when it may.
     */
    private fun versionProblem(job: AutomationJob): String? {
        val (app, pkg) = when (job) {
            is AutomationJob.Fitatu -> SteeredApp.FITATU to Settings.FITATU_PACKAGE
            is AutomationJob.Aaps -> SteeredApp.AAPS to job.packageName
        }
        val check = settings.refreshVersionCheck(app, pkg)
        return if (check.ok) null else "Przerwano: ${check.describe(app)}."
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: android.content.ActivityNotFoundException) {
            error = "Brak przeglądarki do otwarcia $url"
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(SystemSettings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** Opens the system screen that grants this app all-files access. */
    private fun openStorageAccessSettings() {
        try {
            startActivity(Intent(SystemSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: android.content.ActivityNotFoundException) {
            // Some systems only have the list of all apps.
            startActivity(Intent(SystemSettings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private companion object {
        /** History entries read per step of the gallery's endless scroll. */
        const val HISTORY_PAGE = 60

        /** The capture buttons are tall, so they get rounded corners instead of the pill shape. */
        val captureShape = RoundedCornerShape(16.dp)
    }
}
