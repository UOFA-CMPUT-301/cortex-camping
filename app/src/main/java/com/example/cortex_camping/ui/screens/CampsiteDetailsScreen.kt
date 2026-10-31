package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.ui.components.AmenityChip
import com.example.cortex_camping.ui.components.CampsiteLotteryCard
import com.example.cortex_camping.ui.components.CampsitePhoto
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.components.CortexDivider
import com.example.cortex_camping.ui.components.CortexSegmentedToggle
import com.example.cortex_camping.ui.components.GroupedSurface
import com.example.cortex_camping.ui.components.InlineLink
import com.example.cortex_camping.ui.components.LinkListItem
import com.example.cortex_camping.ui.components.LocationMapCard
import com.example.cortex_camping.ui.components.formatRating
import com.example.cortex_camping.ui.theme.CortexTheme

data class LotteryActions(
    val enter: (Lottery) -> Unit,
    val leave: (Lottery) -> Unit,
    val joinWaitlist: (Lottery) -> Unit,
    val leaveWaitlist: (Lottery) -> Unit,
)

/** Figma frames "Campsite Details", "Dates & lottery" and "Lottery full · Join waitlist" */
@Composable
fun CampsiteDetailsScreen(
    campsite: Campsite,
    hostName: String,
    lotteries: List<Lottery>,
    userId: String,
    actions: LotteryActions,
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenRules: () -> Unit,
    onMessageHost: () -> Unit,
    onReviews: () -> Unit,
    onLotteryDetails: (Lottery) -> Unit,
    initialTab: Int = 0,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    Scaffold(
        topBar = { CortexBackTopBar("Campsite details", onBack) },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding()),
        ) {
            CortexSegmentedToggle(
                listOf("About", "Lottery"), tab, { tab = it },
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp), height = 36.dp,
            )
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tab == 0) AboutTab(campsite, hostName, onOpenMap, onOpenRules, onMessageHost, onReviews)
                else LotteryTab(campsite, lotteries, userId, actions, onLotteryDetails)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutTab(
    campsite: Campsite,
    hostName: String,
    onOpenMap: () -> Unit,
    onOpenRules: () -> Unit,
    onMessageHost: () -> Unit,
    onReviews: () -> Unit,
) {
    val colors = CortexTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    var allTags by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CampsitePhoto(campsite, Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(campsite.name, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                Text(campsite.location, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                Text("Max ${campsite.maxOccupancy} guests", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Text("$%.2f CAD per night".format(campsite.price), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
    if (campsite.description.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                campsite.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
            )
            InlineLink(if (expanded) "Show less ‹" else "Read full description ›", { expanded = !expanded })
        }
    }
    if (campsite.tag.isNotEmpty()) {
        val shown = if (allTags) campsite.tag else campsite.tag.take(3)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.Center) {
            shown.forEach { AmenityChip(it) }
            val hidden = campsite.tag.size - 3
            if (hidden > 0) {
                InlineLink(
                    if (allTags) "Show less ‹" else "+$hidden more ›", { allTags = !allTags },
                    Modifier.align(Alignment.CenterVertically).padding(start = 2.dp),
                )
            }
        }
    }
    LocationMapCard(campsite.location, onOpenMap)
    GroupedSurface {
        LinkListItem("Rules & policies", "Site rules, cancellation & owner instructions", onOpenRules)
        CortexDivider()
        LinkListItem("Hosted by $hostName", "Meet your host & send a message", onMessageHost)
        CortexDivider()
        LinkListItem(
            if (campsite.ratingCount == 0) "★ No ratings yet" else "★ ${formatRating(campsite)} · ${campsite.ratingCount} ratings",
            "Read reviews & rate your stay", onReviews,
        )
    }
}

@Composable
private fun LotteryTab(campsite: Campsite, lotteries: List<Lottery>, userId: String, actions: LotteryActions, onDetails: (Lottery) -> Unit) {
    val colors = CortexTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(campsite.name, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Text("${campsite.location} · Max ${campsite.maxOccupancy} guests", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
    if (lotteries.isEmpty()) {
        Text("There are no lotteries for this campsite right now.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
    lotteries.forEach { l ->
        CampsiteLotteryCard(
            lottery = l,
            userId = userId,
            onEnter = { actions.enter(l) },
            onLeave = { actions.leave(l) },
            onJoinWaitlist = { actions.joinWaitlist(l) },
            onLeaveWaitlist = { actions.leaveWaitlist(l) },
            onDetails = { onDetails(l) },
        )
    }
}
