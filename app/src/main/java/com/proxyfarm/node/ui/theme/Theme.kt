package com.proxyfarm.node.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val ProxyGreen     = Color(0xFF00C853)
val ProxyGreenDim  = Color(0xFF1B5E20)
val ProxyBlue      = Color(0xFF2979FF)
val ProxyBlueDim   = Color(0xFF0D47A1)
val ProxyAmber     = Color(0xFFFFAB00)
val ProxyRed       = Color(0xFFD50000)
val SurfaceDark    = Color(0xFF121212)
val SurfaceVariant = Color(0xFF1E1E2E)
val CardDark       = Color(0xFF1A1A2E)

private val DarkColorScheme = darkColorScheme(
    primary = ProxyBlue, onPrimary = Color.White, primaryContainer = ProxyBlueDim,
    secondary = ProxyGreen, onSecondary = Color.Black, secondaryContainer = ProxyGreenDim,
    tertiary = ProxyAmber, background = SurfaceDark, surface = SurfaceVariant,
    surfaceVariant = CardDark, onBackground = Color(0xFFE0E0E0), onSurface = Color(0xFFE0E0E0),
    error = ProxyRed, onError = Color.White
)
private val LightColorScheme = lightColorScheme(
    primary = ProxyBlue, onPrimary = Color.White, primaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFF00897B), onSecondary = Color.White, secondaryContainer = Color(0xFFB2DFDB),
    tertiary = ProxyAmber, background = Color(0xFFF5F5F5), surface = Color.White,
    surfaceVariant = Color(0xFFECEFF1), onBackground = Color(0xFF1A1A1A), onSurface = Color(0xFF1A1A1A),
    error = ProxyRed, onError = Color.White
)

@Composable
fun FleetProxyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
