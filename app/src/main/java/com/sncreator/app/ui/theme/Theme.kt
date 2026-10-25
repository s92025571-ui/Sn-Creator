package com.sncreator.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SnCreatorDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = VoidBlack,
    primaryContainer = DeepRoyalViolet,
    onPrimaryContainer = CrispWhite,
    secondary = SoftLavenderGlow,
    onSecondary = VoidBlack,
    secondaryContainer = ElevatedCardBg,
    onSecondaryContainer = CrispWhite,
    background = VoidBlack,
    onBackground = CrispWhite,
    surface = ObsidianSurface,
    onSurface = CrispWhite,
    surfaceVariant = GlassCardBg,
    onSurfaceVariant = SecondarySlate,
    outline = GlassBorderViolet,
    error = CrimsonError,
    onError = CrispWhite,
    errorContainer = ErrorContainerDark,
    onErrorContainer = CrispWhite
)

@Composable
fun SnCreatorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SnCreatorDarkColorScheme,
        typography = SnCreatorTypography,
        content = content
    )
}
