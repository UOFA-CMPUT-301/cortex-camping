package com.example.cortex_camping.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

enum class CortexButtonStyle {
    Filled,
    Danger,
    DangerSubtle,
    Outlined,
    OutlinedBrand,
    Text,
    TextDanger,
}

enum class CortexButtonSize(val height: Dp, val horizontalPadding: Dp) {
    XSmall(32.dp, 12.dp),
    Small(40.dp, 16.dp),
}

@Composable
fun CortexButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CortexButtonStyle = CortexButtonStyle.Filled,
    size: CortexButtonSize = CortexButtonSize.Small,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    touchTarget: Dp = 48.dp,
) {
    Box(modifier.height(touchTarget), contentAlignment = Alignment.Center, propagateMinConstraints = true) {
        CortexButtonContainer(text, onClick, style, size, leadingIcon, enabled)
    }
}

@Composable
private fun CortexButtonContainer(
    text: String,
    onClick: () -> Unit,
    style: CortexButtonStyle,
    size: CortexButtonSize,
    leadingIcon: ImageVector?,
    enabled: Boolean,
) {
    val colors = CortexTheme.colors
    val padding = PaddingValues(horizontal = size.horizontalPadding)
    val sized = Modifier.requiredHeight(size.height)
    CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
    val content: @Composable () -> Unit = {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
    when (style) {
        CortexButtonStyle.Filled -> Button(
            onClick = onClick, modifier = sized, enabled = enabled, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = CortexPalette.BrandPrimary,
                contentColor = CortexPalette.BrandOnPrimary,
                disabledContainerColor = CortexPalette.OnSurface.copy(alpha = 0.10f),
                disabledContentColor = CortexPalette.OnSurface.copy(alpha = 0.38f),
            ),
        ) { content() }

        CortexButtonStyle.Danger, CortexButtonStyle.DangerSubtle, CortexButtonStyle.Outlined, CortexButtonStyle.OutlinedBrand -> {
            val border = when (style) {
                CortexButtonStyle.Danger -> colors.error
                else -> colors.borderSubtle
            }
            val label = when (style) {
                CortexButtonStyle.Outlined -> colors.textPrimary
                CortexButtonStyle.OutlinedBrand -> CortexPalette.BrandPrimary
                else -> colors.error
            }
            OutlinedButton(
                onClick = onClick, modifier = sized, enabled = enabled, contentPadding = padding,
                border = BorderStroke(1.dp, if (enabled) border else border.copy(alpha = 0.38f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = label,
                    disabledContentColor = label.copy(alpha = 0.38f),
                ),
            ) { content() }
        }

        CortexButtonStyle.Text, CortexButtonStyle.TextDanger -> TextButton(
            onClick = onClick, modifier = sized, enabled = enabled, contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (style == CortexButtonStyle.Text) CortexPalette.BrandPrimary else colors.error,
            ),
        ) { content() }
    }
}
}
