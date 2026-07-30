package com.phantasmaa.panoplia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PanopliaColors {
    val BgRoot       = Color(0xFF0A0A14)
    val BgSurface    = Color(0xFF131320)
    val BgSurfaceHi  = Color(0xFF1C1C2E)
    val TextPrimary  = Color(0xFFE6E6F0)
    val TextSecondary = Color(0xFF9090A8)
    val AccentBlue   = Color(0xFF5B8DEF)
    val AccentOrange = Color(0xFFFF8A4C)
    val ErrorRed     = Color(0xFFE5484D)
    val SuccessGreen = Color(0xFF46A758)
}

private val DarkScheme = darkColorScheme(
    primary = PanopliaColors.AccentBlue,
    onPrimary = Color.White,
    secondary = PanopliaColors.AccentOrange,
    onSecondary = Color.White,
    background = PanopliaColors.BgRoot,
    onBackground = PanopliaColors.TextPrimary,
    surface = PanopliaColors.BgSurface,
    onSurface = PanopliaColors.TextPrimary,
    surfaceVariant = PanopliaColors.BgSurfaceHi,
    onSurfaceVariant = PanopliaColors.TextSecondary,
    error = PanopliaColors.ErrorRed,
    onError = Color.White
)

private val LightScheme = lightColorScheme(
    primary = PanopliaColors.AccentBlue,
    onPrimary = Color.White,
    secondary = PanopliaColors.AccentOrange,
    background = PanopliaColors.BgRoot,
    surface = PanopliaColors.BgSurface
)

@Composable
fun PanopliaTheme(
    darkTheme: Boolean = true, // FORZADO dark por ahora (MIUI bug)
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
