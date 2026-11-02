package com.multispace.ultimate

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B5BD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E2FF),
    onPrimaryContainer = Color(0xFF1B1B6B),
    secondary = Color(0xFF8E5BD6),
    background = Color(0xFFF4F3FB),
    onBackground = Color(0xFF14142B),
    surface = Color.White,
    onSurface = Color(0xFF14142B),
    surfaceVariant = Color(0xFFE9E8F5),
    onSurfaceVariant = Color(0xFF55557A)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9C9DFF),
    onPrimary = Color(0xFF14146B),
    primaryContainer = Color(0xFF2E2F8F),
    onPrimaryContainer = Color(0xFFE3E2FF),
    secondary = Color(0xFFC59CFF),
    background = Color(0xFF0E0F1A),
    onBackground = Color(0xFFEDEDF7),
    surface = Color(0xFF181A2A),
    onSurface = Color(0xFFEDEDF7),
    surfaceVariant = Color(0xFF23263A),
    onSurfaceVariant = Color(0xFFB4B6D0)
)

val Green = Color(0xFF2E9E5B)
val Amber = Color(0xFFE0A100)
val Red = Color(0xFFD64545)
val Gray = Color(0xFF8A8CA5)

@Composable
fun MultiSpaceTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}

@Composable
fun screenBrush(): Brush {
    val c = MaterialTheme.colorScheme
    return Brush.verticalGradient(listOf(c.primary.copy(alpha = 0.16f), c.background, c.background))
}
