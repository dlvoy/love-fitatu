package pl.cukrzycowy.labelbite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The brand salmon; the same value as `ic_launcher_background` in colors.xml. */
val Brand = Color(0xFFF08C4A)

/** A light scheme built around [Brand], with warm neutrals in place of the Material purple baseline. */
private val LabelBiteColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC5),
    onPrimaryContainer = Color(0xFF311300),
    inversePrimary = Color(0xFFFFB787),
    secondary = Color(0xFF77574A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC5),
    onSecondaryContainer = Color(0xFF2C160C),
    tertiary = Color(0xFF6F5C2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE0A6),
    onTertiaryContainer = Color(0xFF251A00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DED5),
    onSurfaceVariant = Color(0xFF534340),
    surfaceTint = Brand,
    inverseSurface = Color(0xFF392E2C),
    inverseOnSurface = Color(0xFFFFEDEA),
    outline = Color(0xFF857370),
    outlineVariant = Color(0xFFD8C2BE),
    surfaceBright = Color(0xFFFFF8F5),
    surfaceDim = Color(0xFFE8D6D3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFCEAE7),
    surfaceContainerHigh = Color(0xFFF7E4E1),
    surfaceContainerHighest = Color(0xFFF1DEDB),
)

@Composable
fun LabelBiteTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = LabelBiteColors, content = content)
