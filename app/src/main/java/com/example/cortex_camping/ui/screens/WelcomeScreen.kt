package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cabin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

/**
 * Welcome screen presented to non-authenticated users.
 * Provides entry points to Register or Log-in.
 */
@Composable
fun WelcomeScreen(
    onRegisterClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.surfaceBase,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Branding / Hero Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(CortexPalette.BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cabin,
                        contentDescription = "Cortex Camping Logo",
                        tint = CortexPalette.BrandPrimary,
                        modifier = Modifier.size(48.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome to Cortex Camping",
                    style = CortexTheme.emphasized.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Discover campsites, enter lotteries, and manage your outdoor adventures seamlessly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // Action Buttons: Register and Log-in
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CortexButton(
                    text = "Register",
                    onClick = onRegisterClick,
                    style = CortexButtonStyle.Filled,
                    modifier = Modifier.fillMaxWidth(),
                )

                CortexButton(
                    text = "Log-in",
                    onClick = onLoginClick,
                    style = CortexButtonStyle.OutlinedBrand,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}


