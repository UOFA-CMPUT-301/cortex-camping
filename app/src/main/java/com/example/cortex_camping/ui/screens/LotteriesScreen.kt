package com.example.cortex_camping.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.ui.components.CortexHomeTopBar
import com.example.cortex_camping.ui.components.CortexSegmentedToggle
import com.example.cortex_camping.ui.components.LotteryEntryCard
import com.example.cortex_camping.ui.components.ResultsHeading
import com.example.cortex_camping.ui.theme.CortexTheme

/** Figma frame "My Lotteries" */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteriesScreen(
    lotteries: List<Lottery>,
    userId: String,
    campsite: (String) -> Campsite?,
    unreadNotifications: Int,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLeave: (Lottery) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val visible = if (tab == 0) lotteries else lotteries.filter { it.status == LotteryStatus.ONGOING }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { CortexHomeTopBar("My Lotteries", onProfileClick, onNotificationsClick, unreadNotifications, scroll) },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CortexSegmentedToggle(listOf("All lotteries", "Ongoing"), tab, { tab = it }, Modifier.padding(bottom = 4.dp)) }
            item { ResultsHeading("Your lottery entries", "${visible.size} lotter${if (visible.size == 1) "y" else "ies"}") }
            if (visible.isEmpty()) item {
                Text("You have no lottery entries here.", style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary)
            }
            items(visible, key = { it.id }) { lottery ->
                val entry = lottery.entryFor(userId) ?: return@items
                val site = campsite(entry.campsiteId) ?: return@items
                LotteryEntryCard(lottery, site, userId, onLeave = { onLeave(lottery) })
            }
        }
    }
}
