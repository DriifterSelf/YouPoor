package com.voidfilio.youpoor.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppDarkColorScheme = darkColorScheme(
    primary            = Coral,
    onPrimary          = White,
    primaryContainer   = CoralContainer,
    onPrimaryContainer = CoralBright,

    secondary          = OnSurfaceMid,
    onSecondary        = Black,
    secondaryContainer = Surface700,
    onSecondaryContainer = OnSurface,

    tertiary           = CoralBright,
    onTertiary         = Black,

    background         = Black,
    onBackground       = OnSurface,

    surface            = Surface900,
    onSurface          = OnSurface,
    surfaceVariant     = Surface800,
    onSurfaceVariant   = OnSurfaceMid,
    surfaceContainer   = Surface700,
    surfaceContainerHigh = Surface800,
    surfaceContainerLow  = Surface900,

    outline            = Surface600,
    outlineVariant     = Surface700,

    error              = ErrorRed,
    errorContainer     = ErrorContainer,
    onError            = White,
    onErrorContainer   = ErrorRed,
)

@Composable
fun YoupoorTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Black.toArgb()
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography  = Typography,
        content     = content,
    )
}
