package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val HighDensityBackground = Color(0xFF090D16)
val HighDensitySurface = Color(0xFF111827)
val HighDensitySurfaceVariant = Color(0xFF1F2937)
val HighDensityBorder = Color(0xFF374151)

val PrimaryCyan = Color(0xFF38BDF8)
val OnPrimaryDark = Color(0xFF0F172A)
val SecondaryEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF0284C7),
    secondary = SecondaryEmerald,
    onSecondary = Color(0xFF064E3B),
    background = HighDensityBackground,
    onBackground = TextPrimary,
    surface = HighDensitySurface,
    onSurface = TextPrimary,
    surfaceVariant = HighDensitySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = HighDensityBorder,
    error = ErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark high-density ops theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = HighDensityBackground.toArgb()
            window.navigationBarColor = HighDensityBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
