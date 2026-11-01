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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.ui.components.BookingOfferCard
import com.example.cortex_camping.ui.components.CortexConfirmDialog
import com.example.cortex_camping.ui.components.CortexHomeTopBar
import com.example.cortex_camping.ui.components.CortexSegmentedToggle
import com.example.cortex_camping.ui.components.PastReservationCard
import com.example.cortex_camping.ui.components.ReservationCard
import com.example.cortex_camping.ui.components.ResultsHeading
import com.example.cortex_camping.ui.theme.CortexTheme

/** Figma frames "My reservations" and "My Reservations - Past" */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    upcoming: List<Reservation>,
    past: List<Reservation>,
    offers: List<Lottery>,
    offerCampsite: (Lottery) -> Campsite?,
    campsite: (String) -> Campsite?,
    unreadNotifications: Int,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMessageOwner: (Reservation) -> Unit,
    onCancel: (Reservation) -> Unit,
    onChooseDates: (Lottery) -> Unit,
    onDecline: (Lottery) -> Unit,
    onRate: (Reservation, Int) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pendingCancel by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDecline by rememberSaveable { mutableStateOf<String?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val count = if (tab == 0) upcoming.size + offers.size else past.size

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { CortexHomeTopBar("My Reservations", onProfileClick, onNotificationsClick, unreadNotifications, scroll) },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { CortexSegmentedToggle(listOf("Upcoming", "Past"), tab, { tab = it }) }
            item {
                ResultsHeading(
                    if (tab == 0) "Your upcoming stays" else "Your past stays",
                    "$count reservation${if (count == 1) "" else "s"}",
                )
            }
            if (count == 0) item {
                Text(
                    if (tab == 0) "No upcoming stays yet. Enter a campsite lottery to book your next trip." else "No past stays yet.",
                    style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            if (tab == 0) {
                items(offers, key = { "offer-${it.id}" }) { lottery ->
                    val site = offerCampsite(lottery) ?: return@items
                    BookingOfferCard(lottery, site, onChooseDates = { onChooseDates(lottery) }, onDecline = { pendingDecline = lottery.id })
                }
                items(upcoming, key = { it.bookingId }) { r ->
                    val site = campsite(r.campsiteId) ?: return@items
                    ReservationCard(r, site, onMessageOwner = { onMessageOwner(r) }, onCancel = { pendingCancel = r.bookingId })
                }
            } else {
                items(past, key = { it.bookingId }) { r ->
                    val site = campsite(r.campsiteId) ?: return@items
                    PastReservationCard(r, site, onRate = { onRate(r, it) })
                }
            }
        }
    }

    pendingCancel?.let { id ->
        val r = upcoming.firstOrNull { it.bookingId == id }
        if (r == null) { pendingCancel = null; return@let }
        CortexConfirmDialog(
            title = "Cancel reservation?",
            text = "${campsite(r.campsiteId)?.name ?: "This campsite"} will be released to other campers. This cannot be undone.",
            confirmLabel = "Cancel reservation",
            dismissLabel = "Keep",
            destructive = true,
            onConfirm = { onCancel(r); pendingCancel = null },
            onDismiss = { pendingCancel = null },
        )
    }
    pendingDecline?.let { id ->
        val lottery = offers.firstOrNull { it.id == id }
        if (lottery == null) { pendingDecline = null; return@let }
        CortexConfirmDialog(
            title = "Decline this offer?",
            text = "${offerCampsite(lottery)?.name ?: "This campsite"} will be released to other campers. This cannot be undone.",
            confirmLabel = "Decline",
            dismissLabel = "Keep",
            destructive = true,
            onConfirm = { onDecline(lottery); pendingDecline = null },
            onDismiss = { pendingDecline = null },
        )
    }
}
