package com.example.cortex_camping.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.NotificationKind
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

@Composable
fun NotificationRow(item: AppNotification, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CortexTheme.colors
    val (icon, tint) = when (item.kind) {
        NotificationKind.BOOKING_OFFER -> Icons.Outlined.Notifications to colors.textPrimary
        NotificationKind.MESSAGE -> Icons.AutoMirrored.Filled.Chat to CortexPalette.BrandPrimary
        NotificationKind.LOTTERY_UPDATED -> Icons.Filled.Edit to CortexPalette.BrandPrimary
        NotificationKind.SUCCESS -> Icons.Filled.Check to CortexPalette.BrandPrimary
        NotificationKind.CANCELLED -> Icons.Filled.Close to colors.error
        NotificationKind.NEUTRAL -> Icons.Filled.Close to colors.textSecondary
        NotificationKind.REVIEW -> Icons.Filled.Star to CortexPalette.OnErrorContainer
        NotificationKind.ALERT -> Icons.Filled.Check to colors.error
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, style = CortexTheme.emphasized.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(formatSentAt(item.sentAt), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Text(item.body, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            if (item.link.isNotEmpty()) Text(item.link, style = MaterialTheme.typography.bodySmall, color = CortexPalette.BrandPrimary)
        }
    }
}

@Composable
fun UnreadDot(modifier: Modifier = Modifier) {
    Box(modifier.size(6.dp).clip(CircleShape).background(CortexPalette.BrandPrimary))
}

@Composable
fun ChatBubble(text: String, meta: String, incoming: Boolean, modifier: Modifier = Modifier) {
    val colors = CortexTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val base = Modifier.widthIn(max = 316.dp).clip(shape)
    val styled = if (incoming) {
        base.background(colors.surfaceBase).border(BorderStroke(1.dp, colors.borderSubtle), shape)
    } else {
        base.background(colors.surfaceDefault)
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End) {
        Column(styled.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
