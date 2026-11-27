package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme
import com.example.cortex_camping.viewmodel.Role
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.cortex_camping.R

// Regular users are able to select whether to interact with the app as a campsite owner or camper/entrant
/*
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
        ) {
            // Header at the TOP
            Text(
                text = "Choose your role",
                style = CortexTheme.emphasized.headlineMedium,
                color = colors.textPrimary,
            )

            Text(
                text = "Select how you would like to interact with Cortex Camping today:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )

            // Smaller top weight pulls elements higher up
            Spacer(modifier = Modifier.weight(0.7f))

            // Twin 1:1 square role cards side-by-side
            if (Role.Owner in roles || Role.User in roles) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Campsite Owner (Left Twin Square)
                    if (Role.Owner in roles) {
                        RoleSelectionSquareCard(
                            title = "Campsite Owner",
                            subtitle = "List campsites, manage availability & host lotteries.",
                            icon = Icons.Outlined.Storefront,
                            onClick = { onSelectRole(Role.Owner) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }

                    // 2. Camper (Right Twin Square)
                    if (Role.User in roles) {
                        RoleSelectionSquareCard(
                            title = "Camper",
                            subtitle = "Browse campsites, enter lotteries & manage bookings.",
                            icon = Icons.Outlined.Person,
                            onClick = { onSelectRole(Role.User) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }

            // Admin card (Full-width rectangle)
            if (Role.Admin in roles) {
                Spacer(modifier = Modifier.height(16.dp))
                RoleSelectionRectangleCard(
                    title = "Admin",
                    subtitle = "Moderate campsites, users, images, and communications.",
                    icon = Icons.Outlined.AdminPanelSettings,
                    onClick = { onSelectRole(Role.Admin) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Larger bottom weight keeps the cards positioned higher up
            Spacer(modifier = Modifier.weight(1.3f))
        }
    }
}
 */

@Composable
fun LoginRoleScreen(
    onSelectRole: (Role) -> Unit,
    roles: List<Role> = listOf(Role.Owner, Role.User),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // 1. Background Image
        Image(
            painter = painterResource(id = R.drawable.bg_role_selection),
            contentDescription = "Role Selection Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Dark Scrim Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // 3. Scaffold
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // Make sure your CortexBackTopBar supports transparent or light content colors if needed
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
            ) {
                Text(
                    text = "Choose your role",
                    style = CortexTheme.emphasized.headlineMedium,
                    color = Color.White,
                )

                Text(
                    text = "Select how you would like to interact with Cortex Camping today:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(modifier = Modifier.weight(0.7f))

                // Role Cards Row
                if (Role.Owner in roles || Role.User in roles) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (Role.Owner in roles) {
                            RoleSelectionSquareCard(
                                title = "Campsite Owner",
                                subtitle = "List campsites, manage availability & host lotteries.",
                                icon = Icons.Outlined.Storefront,
                                onClick = { onSelectRole(Role.Owner) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }

                        if (Role.User in roles) {
                            RoleSelectionSquareCard(
                                title = "Camper",
                                subtitle = "Browse campsites, enter lotteries & manage bookings.",
                                icon = Icons.Outlined.Person,
                                onClick = { onSelectRole(Role.User) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }

                if (Role.Admin in roles) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RoleSelectionRectangleCard(
                        title = "Admin",
                        subtitle = "Moderate campsites, users, images, and communications.",
                        icon = Icons.Outlined.AdminPanelSettings,
                        onClick = { onSelectRole(Role.Admin) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.weight(1.3f))
            }
        }
    }
}

@Composable
private fun RoleSelectionRectangleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceDefault,
        ),
        border = BorderStroke(1.dp, colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CortexPalette.BrandPrimary,
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun RoleSelectionSquareCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.92f), // Frosted glass effect over background photo
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CortexPalette.BrandPrimary,
                modifier = Modifier.size(36.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black, // Dark text for contrast against white card
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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