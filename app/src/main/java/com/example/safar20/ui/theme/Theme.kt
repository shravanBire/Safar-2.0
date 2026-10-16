package com.example.safar20.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Midnight Moto Cockpit (Dark Theme — Default)
private val DarkColorScheme = darkColorScheme(
    primary = RoyalBrass,
    onPrimary = CrankcaseBlack,
    primaryContainer = BronzeRim,
    onPrimaryContainer = WarmIvory,
    secondary = CafeCopper,
    onSecondary = WarmIvory,
    tertiary = PhosphorGreen,
    onTertiary = CrankcaseBlack,
    background = CrankcaseBlack,
    onBackground = WarmIvory,
    surface = GunmetalGauge,
    onSurface = WarmIvory,
    surfaceVariant = BronzeRim,
    onSurfaceVariant = MutedPewter,
    outline = BronzeRim,
    error = SignalRed,
    onError = WarmIvory
)

// Classic Parchment Day-Dial (Light Theme)
private val LightColorScheme = lightColorScheme(
    primary = BritishRacingGreen,
    onPrimary = VintageParchment,
    primaryContainer = SandBorder,
    onPrimaryContainer = EspressoInk,
    secondary = BurnishedBrass,
    onSecondary = VintageParchment,
    tertiary = PhosphorGreen,
    onTertiary = EspressoInk,
    background = VintageParchment,
    onBackground = EspressoInk,
    surface = WarmSand,
    onSurface = EspressoInk,
    surfaceVariant = SandBorder,
    onSurfaceVariant = EspressoInk,
    outline = SandBorder,
    error = SignalRed,
    onError = VintageParchment
)

/**
 * Royal Enfield Retro-Moto Cockpit Theme for SAFAR 2.0.
 * Defaults to Midnight Moto Cockpit dark theme.
 */
@Composable
fun SafarTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Compatibility alias for Safar20Theme.
 */
@Composable
fun Safar20Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    SafarTheme(darkTheme = darkTheme, content = content)
}
