package com.example.cortex_camping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme

@Composable
fun CampsiteListCard(campsite: Campsite, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CortexTheme.colors
    CortexOutlinedCard(modifier = modifier.height(104.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            CampsitePhoto(campsite, Modifier.width(80.dp).fillMaxHeight())
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(campsite.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(campsite.location, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Group, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                        Text("Max ${campsite.maxOccupancy} guests", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = "Rating", tint = colors.rating, modifier = Modifier.size(16.dp))
                        Text(formatRating(campsite), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

fun formatRating(campsite: Campsite): String =
    if (campsite.ratingCount == 0) "New" else "%.1f".format(campsite.rating)

@Composable
fun CampsitePhoto(imageUrl: String?, modifier: Modifier = Modifier) {
    // TODO: load imageUrl with an image loader. This is the empty state until then.
    Box(modifier.background(CortexTheme.colors.surfaceDefault))
}

@Composable
fun CampsitePhoto(campsite: Campsite?, modifier: Modifier = Modifier) {
    CampsitePhoto(campsite?.imageUrl, modifier)
}

@Composable
fun CampsiteSummaryHeader(
    campsite: Campsite,
    subtitle: String,
    meta: String,
    status: String? = null,
    statusColor: Color = CortexPalette.BrandPrimary,
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors
    Row(modifier.fillMaxWidth().height(88.dp)) {
        CampsitePhoto(
            campsite,
            Modifier.width(80.dp).height(88.dp).clip(RoundedCornerShape(topStart = 12.dp)),
        )
        Column(
            Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(campsite.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (status != null) Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

@Composable
fun StarRatingRow(rating: Int, onRate: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            IconButton(onClick = { onRate(star) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Rate $star stars",
                    tint = CortexPalette.BrandPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            if (rating == 0) "Tap to review" else "Thanks for rating",
            style = MaterialTheme.typography.bodySmall,
            color = CortexTheme.colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun LocationMapCard(location: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CortexTheme.colors
    CortexOutlinedCard(modifier = modifier, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Map, contentDescription = null, tint = CortexPalette.BrandPrimary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(location.ifBlank { "Location not added" }, style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp), color = colors.textPrimary)
                Text("View map & directions ›", style = MaterialTheme.typography.bodySmall, color = CortexPalette.BrandPrimary)
            }
        }
    }
}

@Composable
fun AmenityChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(labelColor = CortexTheme.colors.textPrimary),
        border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(enabled = true, borderColor = CortexTheme.colors.borderSubtle),
    )
}
