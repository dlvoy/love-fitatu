package pl.lovefitatu.bridge.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import pl.lovefitatu.bridge.BuildConfig
import pl.lovefitatu.bridge.R
import pl.lovefitatu.bridge.data.MealMapping
import pl.lovefitatu.bridge.data.Settings
import pl.lovefitatu.bridge.ocr.ModelCatalog

private const val OPENROUTER_KEYS_URL = "https://openrouter.ai/settings/keys"

@Composable
fun SettingsScreen(
    settings: Settings,
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var autoSave by remember { mutableStateOf(settings.autoSave) }
    var mapping by remember { mutableStateOf(settings.mealMapping) }
    var aapsPackage by remember { mutableStateOf(settings.aapsPackage) }
    var models by remember { mutableStateOf(ModelCatalog.decode(settings.cachedModels)) }
    var lastPicked by remember { mutableStateOf(settings.lastPickedModel) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }

    // Loads the list once a key is present (after a typing pause), and again on every refresh tap.
    LaunchedEffect(apiKey, refresh) {
        if (apiKey.isBlank() || (refresh == 0 && models.isNotEmpty())) return@LaunchedEffect
        if (refresh == 0) delay(1_000)
        loading = true
        loadError = null
        try {
            models = ModelCatalog.fetchVisionModels(apiKey.trim())
            settings.cachedModels = ModelCatalog.encode(models)
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
        lastPicked = lastPicked,
        notice = when {
            loading -> "Pobieranie listy modeli…"
            models.isEmpty() && apiKey.isBlank() -> "Ustaw klucz API, aby pobrać pełną listę modeli."
            models.isEmpty() -> "Lista modeli nie została pobrana. Użyj odświeżania."
            else -> null
        },
        onPick = {
            model = it
            settings.model = it
            lastPicked = it
            settings.lastPickedModel = it
            showPicker = false
        },
        onDismiss = { showPicker = false },
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; settings.apiKey = it },
                label = { Text("Klucz API OpenRouter") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.weight(1f),
            )
            IconButton({ onOpenUrl(OPENROUTER_KEYS_URL) }) {
                Icon(painterResource(R.drawable.ic_open_in_new), "Otwórz stronę kluczy API OpenRouter")
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Automatycznie naciśnij Zapisz w Fitatu", modifier = Modifier.weight(1f))
            Switch(autoSave, { autoSave = it; settings.autoSave = it })
        }
        OutlinedTextField(
            value = mapping,
            onValueChange = { mapping = it; settings.mealMapping = it },
            label = { Text("Litera kodu = posiłek") },
            supportingText = {
                Text("Posiłki: breakfast, second_breakfast, dinner, snack, supper. Rozpoznano: " +
                    MealMapping.parse(mapping).entries.joinToString { "${it.key} → ${it.value.label}" })
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = aapsPackage,
            onValueChange = { aapsPackage = it; settings.aapsPackage = it },
            label = { Text("Pakiet AAPS") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (serviceEnabled) "Usługa ułatwień dostępu: włączona" else "Usługa ułatwień dostępu: wyłączona",
            color = if (serviceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        OutlinedButton(onOpenAccessibility, modifier = Modifier.fillMaxWidth()) { Text("Otwórz ustawienia ułatwień dostępu") }
        About()
    }
}

@Composable
private fun About() {
    HorizontalDivider(Modifier.padding(top = 12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("O aplikacji", style = MaterialTheme.typography.titleSmall)
        val small = MaterialTheme.typography.bodySmall
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Text("© Dominik Dzienia 2026", style = small, color = muted)
        Text("Wersja ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = small, color = muted)
        Text("Zbudowano: ${BuildConfig.BUILD_TIME}", style = small, color = muted)
        Text("Commit: ${BuildConfig.GIT_COMMIT}", style = small, color = muted)
    }
}
