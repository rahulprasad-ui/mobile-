package com.rivavafi.universal.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val StandardColorScheme = darkColorScheme(
    primary = PrimarySky,
    primaryContainer = PrimaryContainerSky,
    secondary = SecondaryPink,
    tertiary = TertiaryEmerald,
    onPrimary = OnDarkSurface,
    onPrimaryContainer = OnDarkSurface,
    background = AmoledBlack,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    error = VibrantRed
)

val PremiumColorScheme = darkColorScheme(
    primary = PrimarySky,
    primaryContainer = PrimaryContainerSky,
    secondary = SecondaryPink,
    tertiary = TertiaryEmerald,
    onPrimary = OnDarkSurface,
    onPrimaryContainer = OnDarkSurface,
    background = AmoledBlack,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    error = VibrantRed
)

@Composable
fun RivavaTheme(
    isPremium: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isPremium) PremiumColorScheme else StandardColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
