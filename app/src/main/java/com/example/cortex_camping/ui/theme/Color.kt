package com.example.cortex_camping.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Color scheme from Figma. */
object CortexPalette {
    val BrandPrimary = Color(0xFF275D38)
    val BrandOnPrimary = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1F3A2A)
    val TextSecondary = Color(0xFF465E4D)
    val SurfaceBase = Color(0xFFF4F7F2)
    val SurfaceLow = Color(0xFFEDF3E9)
    val SurfaceDefault = Color(0xFFE7F0E4)
    val SurfaceHigh = Color(0xFFE1EADD)
    val SurfaceHighest = Color(0xFFD7E2D4)
    val BorderSubtle = Color(0xFFD7E2D4)
    val BorderDefault = Color(0xFF6B806E)
    val SelectedContainer = Color(0xFFFECB00)
    val OnSelectedContainer = Color(0xFF1F3A2A)
    val AccentContainer = Color(0xFFC3A6CB)
    val OnAccentContainer = Color(0xFF331C31)
    val Error = Color(0xFFB3261E)
    val OnErrorContainer = Color(0xFF852221)
    val Scrim = Color(0xFF000000)
    val OnSurface = Color(0xFF1D1B20)
}

@Immutable
data class CortexExtendedColors(
    val textPrimary: Color = CortexPalette.TextPrimary,
    val textSecondary: Color = CortexPalette.TextSecondary,
    val surfaceBase: Color = CortexPalette.SurfaceBase,
    val surfaceLow: Color = CortexPalette.SurfaceLow,
    val surfaceDefault: Color = CortexPalette.SurfaceDefault,
    val surfaceHigh: Color = CortexPalette.SurfaceHigh,
    val borderSubtle: Color = CortexPalette.BorderSubtle,
    val borderDefault: Color = CortexPalette.BorderDefault,
    val selectedContainer: Color = CortexPalette.SelectedContainer,
    val onSelectedContainer: Color = CortexPalette.OnSelectedContainer,
    val accentContainer: Color = CortexPalette.AccentContainer,
    val onAccentContainer: Color = CortexPalette.OnAccentContainer,
    val error: Color = CortexPalette.Error,
    val rating: Color = CortexPalette.SelectedContainer,
)

val LocalCortexColors = staticCompositionLocalOf { CortexExtendedColors() }
