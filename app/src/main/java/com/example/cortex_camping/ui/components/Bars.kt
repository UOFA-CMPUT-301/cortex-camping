package com.example.cortex_camping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.ui.theme.CortexTheme

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CortexMediumTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val colors = CortexTheme.colors
    val switcher = LocalRoleSwitcher.current
    MediumTopAppBar(
        title = {
            Text(
                title, style = CortexTheme.emphasized.headlineMedium, color = colors.textPrimary,
                modifier = if (switcher != null) Modifier.combinedClickable(onClick = {}, onLongClick = switcher, indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) else Modifier,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.surfaceBase,
            scrolledContainerColor = colors.surfaceDefault,
        ),
        scrollBehavior = scrollBehavior,
    )
}

val LocalRoleSwitcher = androidx.compose.runtime.staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun BellAction(onClick: () -> Unit, unread: Int = 0) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (unread > 0) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = CortexTheme.colors.textPrimary)
        }
    }
}

@Composable
fun BackAction(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CortexTheme.colors.textPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CortexHomeTopBar(
    title: String,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotifications: Int = 0,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CortexMediumTopBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings", tint = CortexTheme.colors.textPrimary)
            }
        },
        actions = { BellAction(onNotificationsClick, unreadNotifications) },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CortexBackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = CortexTheme.colors
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceBase),
    )
}

data class BottomNavItem(val label: String, val icon: ImageVector)

enum class TopLevelTab(val item: BottomNavItem) {
    Browse(BottomNavItem("Browse", Icons.Outlined.LocationOn)),
    Reservations(BottomNavItem("My Reservations", Icons.Outlined.AccountCircle)),
    Lotteries(BottomNavItem("My Lotteries", Icons.Outlined.Notifications)),
}

enum class OwnerTab(val item: BottomNavItem) {
    Campsites(BottomNavItem("My Campsites", Icons.Outlined.LocationOn)),
    Reservations(BottomNavItem("Reservations", Icons.Outlined.AccountCircle)),
    Lotteries(BottomNavItem("Lotteries", Icons.Outlined.Notifications)),
}

@Composable
fun CortexBottomBar(
    selected: TopLevelTab?,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets,
) = CortexNavigationBar(TopLevelTab.entries.map { it.item }, selected?.ordinal, { onSelect(TopLevelTab.entries[it]) }, modifier, windowInsets)

@Composable
fun OwnerBottomBar(
    selected: OwnerTab?,
    onSelect: (OwnerTab) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets,
) = CortexNavigationBar(OwnerTab.entries.map { it.item }, selected?.ordinal, { onSelect(OwnerTab.entries[it]) }, modifier, windowInsets)

@Composable
fun CortexNavigationBar(
    items: List<BottomNavItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets,
) {
    val colors = CortexTheme.colors
    // Material's NavigationBar is at least 80dp tall but the design is 64dp, so the items
    // go in a plain row instead.
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceDefault)
            .windowInsetsPadding(windowInsets)
            .height(64.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(index) },
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = {
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSelectedContainer,
                    selectedTextColor = colors.textPrimary,
                    indicatorColor = colors.selectedContainer,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                ),
            )
        }
    }
}
