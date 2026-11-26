package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme
import com.example.cortex_camping.viewmodel.Role

// Regular users are able to select whether to interact with the app as a campsite owner or camper/entrant
@Composable
fun LoginRoleScreen(
    onSelectRole: (Role) -> Unit,
    roles: List<Role> = listOf(Role.Owner, Role.User),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.surfaceBase,
        topBar = {
            CortexBackTopBar(
                title = "Log In",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Choose your role",
                style = CortexTheme.emphasized.headlineMedium,
                color = colors.textPrimary,
            )

            Text(
                text = "Select how you would like to interact with Cortex Camping today:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (Role.Owner in roles) {
                RoleSelectionCard(
                    title = "Campsite Owner",
                    subtitle = "List your campsites, manage availability, and host lotteries for campers.",
                    icon = Icons.Outlined.Storefront,
                    onClick = { onSelectRole(Role.Owner) },
                )
            }

            if (Role.User in roles) {
                RoleSelectionCard(
                    title = "Camper",
                    subtitle = "Browse campsites, enter lotteries, and manage your reservations.",
                    icon = Icons.Outlined.Person,
                    onClick = { onSelectRole(Role.User) },
                )
            }

            if (Role.Admin in roles) {
                RoleSelectionCard(
                    title = "Admin",
                    subtitle = "Moderate campsites, users, images, and communications.",
                    icon = Icons.Outlined.AdminPanelSettings,
                    onClick = { onSelectRole(Role.Admin) },
                )
            }
        }
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceDefault,
        ),
        border = BorderStroke(1.dp, colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CortexPalette.BrandPrimary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Preview(name = "Login Role Screen", showBackground = true)
@Composable
private fun LoginRoleScreenPreview() {
    CortexTheme {
        LoginRoleScreen(
            onSelectRole = {},
            onBack = {},
        )
    }
}

@Preview(name = "Login Role Screen - Admin", showBackground = true)
@Composable
private fun LoginRoleScreenAdminPreview() {
    CortexTheme {
        LoginRoleScreen(
            onSelectRole = {},
            roles = Role.entries,
            onBack = {},
        )
    }
}