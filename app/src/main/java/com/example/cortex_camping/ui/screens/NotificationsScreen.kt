package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonSize
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexDivider
import com.example.cortex_camping.ui.components.GroupedSurface
import com.example.cortex_camping.ui.components.NotificationRow
import com.example.cortex_camping.ui.components.UnreadDot
import com.example.cortex_camping.ui.components.formatDate
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

/** Figma frame "Notifications" for campers */
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDownloadCsv: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val unread = notifications.count { !it.read }
    NotificationFeedScreen(
        notifications = notifications, onBack = onBack,
        summary = "$unread unread · ${notifications.size} updates", showDot = unread > 0,
        onNotificationClick = onNotificationClick, contentPadding = contentPadding,
        controls = {
            CortexButton("Mark all read", onMarkAllRead, style = CortexButtonStyle.Text, size = CortexButtonSize.XSmall, enabled = unread > 0, touchTarget = 32.dp)
            Text("|", color = CortexPalette.BrandPrimary, style = MaterialTheme.typography.labelLarge)
            CortexButton("Download CSV", onDownloadCsv, style = CortexButtonStyle.Text, size = CortexButtonSize.XSmall, touchTarget = 32.dp)
        },
    )
}

@Composable
fun NotificationFeedScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    summary: String,
    showDot: Boolean,
    onNotificationClick: (AppNotification) -> Unit,
    controls: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = CortexTheme.colors
    val today = notifications.filter { it.isToday }
    val earlier = notifications.filterNot { it.isToday }

    Scaffold(
        topBar = { CortexBackTopBar("Notifications", onBack) },
        containerColor = colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (header != null) item { Box(Modifier.padding(top = 12.dp, bottom = 8.dp)) { header() } }
            item {
                Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showDot) UnreadDot()
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                        modifier = Modifier.padding(start = if (showDot) 6.dp else 0.dp).weight(1f),
                    )
                    controls()
                }
            }
            if (today.isNotEmpty()) {
                item { Text("Today · ${formatDate(java.time.LocalDate.now())}", style = CortexTheme.emphasized.bodySmall, color = CortexPalette.BrandPrimary) }
                item { Group(today, colors.surfaceLow, onNotificationClick) }
            }
            if (earlier.isNotEmpty()) {
                item { Text("Earlier", style = CortexTheme.emphasized.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp)) }
                item { Group(earlier, colors.surfaceBase, onNotificationClick) }
            }
        }
    }
}

@Composable
private fun Group(items: List<AppNotification>, background: androidx.compose.ui.graphics.Color, onClick: (AppNotification) -> Unit) {
    GroupedSurface(background = background) {
        items.forEachIndexed { i, n ->
            if (i > 0) CortexDivider()
            NotificationRow(n, onClick = { onClick(n) })
        }
    }
}
