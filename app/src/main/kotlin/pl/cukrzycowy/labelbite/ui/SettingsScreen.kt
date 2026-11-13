package pl.cukrzycowy.labelbite.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import pl.cukrzycowy.labelbite.BuildConfig
import pl.cukrzycowy.labelbite.R
import pl.cukrzycowy.labelbite.automation.AppVersions
import pl.cukrzycowy.labelbite.automation.SteeredApp
import pl.cukrzycowy.labelbite.data.Catering
import pl.cukrzycowy.labelbite.data.MealMapping
import pl.cukrzycowy.labelbite.data.Settings
import pl.cukrzycowy.labelbite.ocr.LlmProvider
import pl.cukrzycowy.labelbite.ocr.ModelCatalog

@Composable
fun SettingsScreen(
    settings: Settings,
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(R.drawable.ic_smart_toy, "Rozpoznawanie etykiety") { LlmSettings(settings, onOpenUrl) }
        Section(R.drawable.ic_restaurant, "Catering dietetyczny") { CateringSettings(settings, onOpenUrl) }
        Section(R.drawable.ic_tune, "Fitatu i AAPS") { AutomationSettings(settings, serviceEnabled, onOpenAccessibility) }
        Section(R.drawable.ic_info, "O aplikacji") { About() }
    }
}

// ------------------------------------------------------------------ sections

@Composable
private fun LlmSettings(settings: Settings, onOpenUrl: (String) -> Unit) {
    var provider by remember { mutableStateOf(settings.provider) }
    // Every provider keeps its own key, model and list, so all of this restarts when the provider changes.
    var apiKey by remember(provider) { mutableStateOf(settings.apiKeyFor(provider)) }
    var model by remember(provider) { mutableStateOf(settings.modelFor(provider)) }
    var models by remember(provider) { mutableStateOf(ModelCatalog.decode(settings.cachedModelsFor(provider))) }
    var lastPicked by remember(provider) { mutableStateOf(settings.lastPickedModelFor(provider)) }
    var loading by remember(provider) { mutableStateOf(false) }
    var loadError by remember(provider) { mutableStateOf<String?>(null) }
    var refresh by remember(provider) { mutableIntStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }

    // Loads the list once a key is present (after a typing pause), and again on every refresh tap.
    LaunchedEffect(provider, apiKey, refresh) {
        if (apiKey.isBlank() || (refresh == 0 && models.isNotEmpty())) return@LaunchedEffect
        if (refresh == 0) delay(1_000)
        loading = true
        loadError = null
        try {
            models = ModelCatalog.fetchModels(provider, apiKey.trim())
            settings.setCachedModels(provider, ModelCatalog.encode(models))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = "Nie udało się pobrać listy modeli: ${e.message ?: e.javaClass.simpleName}"
        } finally {
            loading = false
        }
    }

    if (showPicker) ModelPickerDialog(
        models = models,
        selected = model,
        preferred = provider.preferred,
        allLabel = if (provider.visionOnlyList) "Wszystkie modele z obsługą obrazów" else "Wszystkie modele",
        lastPicked = lastPicked,
        notice = when {
            loading -> "Pobieranie listy modeli…"
            models.isEmpty() && apiKey.isBlank() -> "Ustaw klucz API, aby pobrać pełną listę modeli."
            models.isEmpty() -> "Lista modeli nie została pobrana. Użyj odświeżania."
            !provider.visionOnlyList -> "${provider.label} nie podaje, które modele czytają obrazy. Wybierz model z obsługą obrazów."
            else -> null
        },
        onPick = {
            model = it
            settings.setModel(provider, it)
            lastPicked = it
            settings.setLastPickedModel(provider, it)
            showPicker = false
        },
        onDismiss = { showPicker = false },
    )

    ChoiceField(
        label = "Dostawca modelu",
        options = LlmProvider.entries.map { Choice(it, it.label, it.icon) },
        selected = provider,
        onSelect = { provider = it; settings.provider = it },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; settings.setApiKey(provider, it) },
            label = { Text("Klucz API ${provider.label}") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.weight(1f),
        )
        IconButton({ onOpenUrl(provider.keysUrl) }) {
            Icon(painterResource(R.drawable.ic_open_in_new), "Otwórz stronę kluczy API ${provider.label}")
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = model,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // A read-only text field swallows clicks, so the tap target sits on top of it.
            Box(Modifier.matchParentSize().clickable { showPicker = true })
        }
        if (loading) CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
        else IconButton({ refresh++ }, enabled = apiKey.isNotBlank()) {
            Icon(painterResource(R.drawable.ic_refresh), "Odśwież listę modeli")
        }
    }
    loadError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun CateringSettings(settings: Settings, onOpenUrl: (String) -> Unit) {
    var catering by remember { mutableStateOf(settings.catering) }
    var mapping by remember(catering) { mutableStateOf(settings.mealMappingFor(catering)) }
    var customPrompt by remember { mutableStateOf(settings.customPrompt) }
    var advanced by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        ChoiceField(
            label = "Catering",
            options = Catering.entries.map { Choice(it, it.label, it.icon, it.tintIcon) },
            selected = catering,
            onSelect = { catering = it; settings.catering = it },
            modifier = Modifier.weight(1f),
        )
        catering.siteUrl?.let { url ->
            IconButton({ onOpenUrl(url) }) {
                Icon(painterResource(R.drawable.ic_open_in_new), "Otwórz stronę ${catering.label}")
            }
        }
    }
    // The built-in prompt of a known catering is not shown; only a custom one is written by the user.
    if (catering == Catering.CUSTOM) OutlinedTextField(
        value = customPrompt,
        onValueChange = { customPrompt = it; settings.customPrompt = it },
        label = { Text("Opis etykiety dla modelu") },
        supportingText = {
            Text(
                "Opisz, co jest na etykiecie i przy jakich napisach stoją wartości. Wymagany format " +
                    "odpowiedzi aplikacja dopisuje sama. Puste pole przywraca opis domyślny."
            )
        },
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
    )
    AdvancedHeader(advanced) { advanced = !advanced }
    if (advanced) OutlinedTextField(
        value = mapping,
        onValueChange = { mapping = it; settings.setMealMapping(catering, it) },
        label = { Text("Litera kodu = posiłek") },
        supportingText = {
            Text("Posiłki: breakfast, second_breakfast, dinner, snack, supper. Rozpoznano: " +
                MealMapping.parse(mapping).entries.joinToString { "${it.key} → ${it.value.label}" })
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AutomationSettings(settings: Settings, serviceEnabled: Boolean, onOpenAccessibility: () -> Unit) {
    var autoSave by remember { mutableStateOf(settings.autoSave) }
    var aapsPackage by remember { mutableStateOf(settings.aapsPackage) }
    var advanced by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Automatycznie naciśnij Zapisz w Fitatu", modifier = Modifier.weight(1f))
        Switch(autoSave, { autoSave = it; settings.autoSave = it })
    }
    AccessibilityButton(serviceEnabled, onOpenAccessibility)
    VersionCheckRow(settings, SteeredApp.FITATU, Settings.FITATU_PACKAGE)
    VersionCheckRow(settings, SteeredApp.AAPS, aapsPackage.trim().ifBlank { Settings.DEFAULT_AAPS_PACKAGE })
    AdvancedHeader(advanced) { advanced = !advanced }
    if (advanced) OutlinedTextField(
        value = aapsPackage,
        onValueChange = { aapsPackage = it; settings.aapsPackage = it },
        label = { Text("Pakiet AAPS") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * A state button for the installed version of [app]: green when it can be steered, red when it is
 * missing or too old, yellow while it still has to be checked. Tapping it checks again.
 */
@Composable
private fun VersionCheckRow(settings: Settings, app: SteeredApp, packageName: String) {
    val context = LocalContext.current
    var stored by remember(app) { mutableStateOf(settings.versionCheck(app)) }
    // Opening Settings checks what is installed now. Typing another AAPS package does not: a half
    // typed name is not worth recording, so that one waits for a tap.
    LaunchedEffect(app) { stored = settings.refreshVersionCheck(app, packageName) }
    // A result for another package (the AAPS package was changed since) says nothing about this one.
    val check = stored?.takeIf { it.packageName == packageName }

    StateButton(
        state = when {
            check == null -> ButtonState.TO_CHECK
            check.ok -> ButtonState.OK
            else -> ButtonState.BAD
        },
        text = check?.summary(app) ?: "Sprawdź wersję ${app.label}",
        onClick = { stored = AppVersions.check(context, app, packageName).also { settings.setVersionCheck(app, it) } },
    )
}

@Composable
private fun About() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val small = MaterialTheme.typography.bodySmall
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Text("© Dominik Dzienia 2026", style = small, color = muted)
        Text("Wersja ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = small, color = muted)
        Text("Zbudowano: ${BuildConfig.BUILD_TIME}", style = small, color = muted)
        Text("Commit: ${BuildConfig.GIT_COMMIT}", style = small, color = muted)
    }
}

// ---------------------------------------------------------------- components

/** A titled card that groups related settings. */
@Composable
private fun Section(icon: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

/** The header of a collapsible group of rarely changed settings. */
@Composable
private fun AdvancedHeader(expanded: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
    ) {
        Text("Zaawansowane", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Icon(
            painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
            if (expanded) "Zwiń" else "Rozwiń",
        )
    }
}

/** What a [StateButton] reports, with its fill colour, content colour and icon. */
private enum class ButtonState(val container: Color, val content: Color, val icon: Int) {
    OK(Color(0xFF2E7D32), Color.White, R.drawable.ic_check_circle),
    NEEDED(Color(0xFFE65100), Color.White, R.drawable.ic_warning),
    BAD(Color(0xFFC62828), Color.White, R.drawable.ic_warning),
    TO_CHECK(Color(0xFFFBC02D), Color(0xFF1F1F1F), R.drawable.ic_refresh),
}

/** A full-width button that shows a state by its colour and icon and acts on it when tapped. */
@Composable
private fun StateButton(state: ButtonState, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = state.container, contentColor = state.content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(painterResource(state.icon), null, modifier = Modifier.size(20.dp))
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Shows the state of the accessibility service and opens its system settings: green with a check
 * mark while the service is on, orange with a warning while it still has to be turned on.
 */
@Composable
fun AccessibilityButton(enabled: Boolean, onClick: () -> Unit) {
    StateButton(
        state = if (enabled) ButtonState.OK else ButtonState.NEEDED,
        text = if (enabled) "Usługa ułatwień dostępu włączona" else "Włącz usługę ułatwień dostępu",
        onClick = onClick,
    )
}

/** One entry of a [ChoiceField]. [tint] is false for full-colour logos, which keep their own colours. */
private data class Choice<T>(val value: T, val label: String, val icon: Int, val tint: Boolean = true)

@Composable
private fun ChoiceIcon(choice: Choice<*>) {
    Icon(
        painterResource(choice.icon), null,
        tint = if (choice.tint) LocalContentColor.current else Color.Unspecified,
        modifier = Modifier.size(24.dp),
    )
}

/** A field showing the selected option with its icon; tapping it opens a menu of all options. */
@Composable
private fun <T> ChoiceField(
    label: String,
    options: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.first { it.value == selected }
    Box(modifier) {
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { ChoiceIcon(current) },
            trailingIcon = { Icon(painterResource(R.drawable.ic_arrow_drop_down), null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // A read-only text field swallows clicks, so the tap target sits on top of it.
        Box(Modifier.matchParentSize().clickable(onClickLabel = "Zmień: $label") { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in options) DropdownMenuItem(
                text = { Text(option.label) },
                leadingIcon = { ChoiceIcon(option) },
                trailingIcon = {
                    if (option.value == selected) Text("✓", color = MaterialTheme.colorScheme.primary)
                },
                onClick = { open = false; onSelect(option.value) },
            )
        }
    }
}
