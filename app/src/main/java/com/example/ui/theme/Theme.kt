package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalDarkColorScheme = darkColorScheme(
    primary = CryptoCyan,
    onPrimary = Color.Black,
    primaryContainer = CryptoCyanSoft,
    onPrimaryContainer = CryptoCyan,
    secondary = BullishGreen,
    onSecondary = Color.Black,
    secondaryContainer = BullishGreenSoft,
    onSecondaryContainer = BullishGreenLight,
    tertiary = GoldAccent,
    onTertiary = Color.Black,
    tertiaryContainer = GoldAccentSoft,
    onTertiaryContainer = GoldAccent,
    background = TerminalBackground,
    onBackground = NeutralTextPrimary,
    surface = TerminalSurface,
    onSurface = NeutralTextPrimary,
    surfaceVariant = TerminalSurfaceElevated,
    onSurfaceVariant = NeutralTextSecondary,
    outline = TerminalSurfaceBorder,
    error = BearishRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TerminalDarkColorScheme,
        typography = Typography,
        content = content
    )
}

