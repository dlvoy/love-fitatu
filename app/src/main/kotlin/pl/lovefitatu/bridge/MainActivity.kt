package pl.lovefitatu.bridge

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.lovefitatu.bridge.automation.AutomationJob
import pl.lovefitatu.bridge.automation.AutomationStatus
import pl.lovefitatu.bridge.automation.BridgeAccessibilityService
import pl.lovefitatu.bridge.automation.SteeredApp
import pl.lovefitatu.bridge.data.History
import pl.lovefitatu.bridge.data.LabelRecord
import pl.lovefitatu.bridge.data.SecureStore
import pl.lovefitatu.bridge.data.Settings
import pl.lovefitatu.bridge.ocr.ImagePrep
import pl.lovefitatu.bridge.ocr.LabelReader
import pl.lovefitatu.bridge.ocr.LabelResult
import pl.lovefitatu.bridge.ocr.LlmException
import pl.lovefitatu.bridge.ui.AccessibilityButton
import pl.lovefitatu.bridge.ui.Gallery
import pl.lovefitatu.bridge.ui.RecordDialog
import pl.lovefitatu.bridge.ui.ReviewData
import pl.lovefitatu.bridge.ui.ReviewScreen
import pl.lovefitatu.bridge.ui.SettingsScreen
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    private enum class Screen { MAIN, REVIEW, SETTINGS }

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
    private var records by mutableStateOf(emptyList<LabelRecord>())
    private var galleryGrid by mutableStateOf(true)
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

        // The store stays unlocked for the life of the process, so a recreated activity skips the prompt.
        if (!SecureStore.unlocked && !SecureStore.isBiometricMode(this)) SecureStore.unlockPlain(this)
        if (SecureStore.unlocked) onUnlocked()

        setContent {
            MaterialTheme {
                if (!unlocked) {
                    LockScreen()
                    return@MaterialTheme
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
                opened?.let { record ->
                    RecordDialog(record, onReuse = { reopen(record) }, onDismiss = { opened = null })
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when {
                                        selecting -> "Zaznaczono: ${selected.size}"
                                        screen == Screen.SETTINGS -> "Ustawienia"
                                        else -> "Love → Fitatu"
                                    }
                                )
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
                                if (selecting) Unit
                                else if (screen == Screen.MAIN) TextButton({ screen = Screen.SETTINGS }) { Text("Ustawienia") }
                                else TextButton({ screen = Screen.MAIN }) { Text("Gotowe") }
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
                            onClick = { if (selected.isEmpty()) opened = it else toggleSelected(it) },
                            onLongClick = ::toggleSelected,
                            modifier = Modifier.weight(1f),
                        )
                    } else Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = base.verticalScroll(rememberScrollState()).padding(16.dp),
                    ) {
                        if (screen == Screen.SETTINGS) SettingsScreen(settings, serviceEnabled, ::openAccessibilitySettings, ::openUrl)
                        else review?.let { ReviewScreen(it, settings, status, ::run) }
                        Notices()
                    }
                }
            }
        }
    }

    @Composable
    private fun LockScreen() {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(32.dp),
        ) {
            Text("Love → Fitatu", style = MaterialTheme.typography.headlineSmall)
            Text("Konfiguracja jest zabezpieczona biometrią.")
            lockMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(::promptUnlock) { Text("Odblokuj") }
        }
    }

    @Composable
    private fun MainContent(status: String?) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(::launchCamera, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Zrób zdjęcie") }
            OutlinedButton(
                { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) { Text("Z galerii") }
        }
        if (busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Text("Odczytywanie etykiety…")
        }
        if (review != null && !busy) TextButton({ screen = Screen.REVIEW }) { Text("Wróć do ostatniej etykiety") }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    @Composable
    private fun Notices() {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!serviceEnabled && screen != Screen.SETTINGS) AccessibilityButton(false, ::openAccessibilitySettings)
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = BridgeAccessibilityService.instance != null
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
            .setTitle("Love → Fitatu")
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
        opened = null
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

    private fun reloadHistory() {
        lifecycleScope.launch { records = withContext(Dispatchers.IO) { History.list(this@MainActivity) } }
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
}
