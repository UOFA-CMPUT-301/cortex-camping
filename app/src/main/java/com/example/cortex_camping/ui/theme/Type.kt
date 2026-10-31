package com.example.cortex_camping.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Roboto = FontFamily.Default

private fun style(size: Int, line: Int, tracking: Double, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = Roboto,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

val CortexTypography = Typography(
    headlineMedium = style(28, 36, 0.0),
    headlineSmall = style(24, 32, 0.0),
    titleLarge = style(22, 28, 0.0),
    titleMedium = style(16, 24, 0.15, FontWeight.Medium),
    bodyLarge = style(16, 24, 0.5),
    bodyMedium = style(14, 20, 0.25),
    bodySmall = style(12, 16, 0.4),
    labelLarge = style(14, 20, 0.1, FontWeight.Medium),
    labelMedium = style(12, 16, 0.5, FontWeight.Medium),
)

@Immutable
data class CortexEmphasizedType(
    val headlineMedium: TextStyle = style(28, 36, 0.0, FontWeight.Medium),
    val bodyMedium: TextStyle = style(14, 20, 0.25, FontWeight.Medium),
    val bodySmall: TextStyle = style(12, 16, 0.4, FontWeight.Medium),
)

val LocalCortexEmphasizedType = staticCompositionLocalOf { CortexEmphasizedType() }
