package com.example.cortex_camping.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

private val CortexColorScheme = lightColorScheme(
    primary = CortexPalette.BrandPrimary,
    onPrimary = CortexPalette.BrandOnPrimary,
    secondaryContainer = CortexPalette.SelectedContainer,
    onSecondaryContainer = CortexPalette.OnSelectedContainer,
    tertiaryContainer = CortexPalette.AccentContainer,
    onTertiaryContainer = CortexPalette.OnAccentContainer,
    background = CortexPalette.SurfaceBase,
    onBackground = CortexPalette.TextPrimary,
    surface = CortexPalette.SurfaceBase,
    onSurface = CortexPalette.TextPrimary,
    onSurfaceVariant = CortexPalette.TextSecondary,
    surfaceContainerLowest = CortexPalette.SurfaceBase,
    surfaceContainerLow = CortexPalette.SurfaceLow,
    surfaceContainer = CortexPalette.SurfaceDefault,
    surfaceContainerHigh = CortexPalette.SurfaceHigh,
    surfaceContainerHighest = CortexPalette.SurfaceHighest,
    outline = CortexPalette.BorderDefault,
    outlineVariant = CortexPalette.BorderSubtle,
    error = CortexPalette.Error,
    onErrorContainer = CortexPalette.OnErrorContainer,
    scrim = CortexPalette.Scrim,
)

val CortexShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CortexTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCortexColors provides CortexExtendedColors(),
        LocalCortexEmphasizedType provides CortexEmphasizedType(),
    ) {
        MaterialTheme(
            colorScheme = CortexColorScheme,
            typography = CortexTypography,
            shapes = CortexShapes,
            content = content,
        )
    }
}

object CortexTheme {
    val colors: CortexExtendedColors
        @Composable @ReadOnlyComposable get() = LocalCortexColors.current
    val emphasized: CortexEmphasizedType
        @Composable @ReadOnlyComposable get() = LocalCortexEmphasizedType.current
}
