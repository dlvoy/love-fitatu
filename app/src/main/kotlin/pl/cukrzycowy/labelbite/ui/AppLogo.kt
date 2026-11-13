package pl.cukrzycowy.labelbite.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.cukrzycowy.labelbite.R

/** The app logo: the monster biting a label, drawn without a background. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_logo), null, modifier.size(size))
}

/** A small grey note under a thin line, with an info icon; for remarks at the bottom of a screen. */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.outline
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        HorizontalDivider(thickness = 0.5.dp, color = muted)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(painterResource(R.drawable.ic_info), null, tint = muted, modifier = Modifier.size(14.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
