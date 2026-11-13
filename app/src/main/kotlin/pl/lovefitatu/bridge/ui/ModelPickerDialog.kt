package pl.lovefitatu.bridge.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pl.lovefitatu.bridge.ocr.ModelInfo

/** Preferred ids for the picker: the provider's fixed ones, then the user's last pick if it is a different model. */
fun preferredModels(fixed: List<String>, lastPicked: String): List<String> =
    (fixed + lastPicked).filter { it.isNotBlank() }.distinct()

/** A typed model id can be used as is when it looks like an id and the list does not have it. */
fun canUseTypedId(typed: String, matches: List<ModelInfo>): Boolean =
    typed.isNotEmpty() && typed.none { it.isWhitespace() } && matches.none { it.id == typed }

fun filterModels(models: List<ModelInfo>, query: String): List<ModelInfo> {
    val q = query.trim()
    return if (q.isEmpty()) models
    else models.filter { it.id.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
}

@Composable
fun ModelPickerDialog(
    models: List<ModelInfo>,
    selected: String,
    preferred: List<String>,
    allLabel: String,
    lastPicked: String,
    notice: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val byId = remember(models) { models.associateBy { it.id } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.fillMaxHeight(0.9f)) {
            Column(Modifier.padding(vertical = 16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Szukaj modelu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                notice?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp, 8.dp))
                }
                LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                    if (query.isBlank()) {
                        item { SectionLabel("Preferowane") }
                        items(preferredModels(preferred, lastPicked)) { id ->
                            ModelRow(id, byId[id]?.name, id == selected) { onPick(id) }
                        }
                        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                        item { SectionLabel("$allLabel (${models.size})") }
                        items(models, key = { it.id }) { ModelRow(it.id, it.name, it.id == selected) { onPick(it.id) } }
                    } else {
                        val matches = filterModels(models, query)
                        items(matches, key = { it.id }) { ModelRow(it.id, it.name, it.id == selected) { onPick(it.id) } }
                        val typed = query.trim()
                        // Lets a model id be entered by hand when the list is missing or out of date.
                        val usable = canUseTypedId(typed, matches)
                        if (usable) item { ModelRow(typed, "Użyj wpisanego id", typed == selected) { onPick(typed) } }
                        if (matches.isEmpty() && !usable) item { SectionLabel("Brak wyników") }
                    }
                }
                TextButton(onDismiss, modifier = Modifier.align(Alignment.End).padding(horizontal = 8.dp)) { Text("Anuluj") }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun ModelRow(id: String, name: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(id, style = MaterialTheme.typography.bodyLarge)
            if (name != null && name != id) Text(name, style = MaterialTheme.typography.bodySmall)
        }
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary)
    }
}
