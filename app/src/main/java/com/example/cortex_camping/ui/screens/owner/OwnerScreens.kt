package com.example.cortex_camping.ui.screens.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.ui.components.ActionPair
import com.example.cortex_camping.ui.components.AvailabilityDialog
import com.example.cortex_camping.ui.components.BellAction
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonSize
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexConfirmDialog
import com.example.cortex_camping.ui.components.CortexMediumTopBar
import com.example.cortex_camping.ui.components.CortexSegmentedToggle
import com.example.cortex_camping.ui.components.CampsitePhoto
import com.example.cortex_camping.ui.components.InfoDialog
import com.example.cortex_camping.ui.components.formatDate
import com.example.cortex_camping.ui.components.formatRange
import com.example.cortex_camping.ui.components.formatRating
import com.example.cortex_camping.ui.components.ManagementCard
import com.example.cortex_camping.ui.components.QrCodeDialog
import com.example.cortex_camping.ui.components.ResultsHeading
import com.example.cortex_camping.ui.components.SectionTitle
import com.example.cortex_camping.ui.components.nightsGuests
import com.example.cortex_camping.ui.screens.NotificationFeedScreen
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.LocalDate
import java.time.LocalDateTime

data class GuestReservation(val reservation: Reservation, val guestName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerTabScaffold(
    title: String,
    onNotifications: () -> Unit,
    unread: Int,
    content: LazyListContent,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { CortexMediumTopBar(title, actions = { BellAction(onNotifications, unread) }, scrollBehavior = scroll) },
        containerColor = CortexTheme.colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
private typealias LazyListContent = androidx.compose.foundation.lazy.LazyListScope.() -> Unit

@Composable
private fun HeadingWithAction(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(title, Modifier.weight(1f))
        CortexButton(action, onAction, style = CortexButtonStyle.OutlinedBrand)
    }
}

/** Figma frame "My Campsites" */
@Composable
fun OwnerCampsitesScreen(
    campsites: List<Campsite>,
    hasOngoingLottery: (Campsite) -> Boolean,
    bookedDates: (Campsite) -> Set<LocalDate>,
    unread: Int,
    onNotifications: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Campsite) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var qrFor by rememberSaveable { mutableStateOf<String?>(null) }
    var datesFor by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = if (tab == 0) campsites else campsites.filter(hasOngoingLottery)

    OwnerTabScaffold("My Campsites", onNotifications, unread) {
        item { CortexSegmentedToggle(listOf("All campsites", "Lottery ongoing"), tab, { tab = it }, Modifier.padding(bottom = 4.dp)) }
        item { HeadingWithAction("Your campsites · ${visible.size}", "Add campsite", onAdd) }
        if (visible.isEmpty()) item {
            Text("No campsites yet. Add one to start taking bookings.", style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary)
        }
        items(visible, key = { it.campsiteId }) { site ->
            ManagementCard(
                title = site.name, subtitle = site.location,
                meta = "Max ${site.maxOccupancy} guests · ${formatRating(site)} rating",
                leading = { m -> CampsitePhoto(site, m) },
                trailing = { CortexButton("QR code", { qrFor = site.campsiteId }, size = CortexButtonSize.XSmall, touchTarget = 32.dp) },
                footerPadding = 4.dp,
            ) {
                CortexButton("Edit campsite", { onEdit(site) }, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand, touchTarget = 40.dp)
                CortexButton("View booked dates", { datesFor = site.campsiteId }, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand, touchTarget = 40.dp)
            }
        }
    }
    qrFor?.let { id -> campsites.firstOrNull { it.campsiteId == id }?.let { QrCodeDialog(it) { qrFor = null } } ?: run { qrFor = null } }
    datesFor?.let { id ->
        val site = campsites.firstOrNull { it.campsiteId == id }
        if (site == null) datesFor = null
        else AvailabilityDialog("${site.name} · booked dates", bookedDates(site), editable = false, onDismiss = { datesFor = null })
    }
}

/** Figma frame "Reservations" in Owner */
@Composable
fun OwnerReservationsScreen(
    reservations: List<GuestReservation>,
    campsite: (String) -> Campsite?,
    unread: Int,
    onNotifications: () -> Unit,
    onMessage: (GuestReservation) -> Unit,
    onSetStatus: (GuestReservation, ReservationStatus) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val visible = reservations.filter { (r) ->
        val past = r.checkOut.isBefore(today)
        when (tab) {
            0 -> !past && r.status != ReservationStatus.CANCELLED
            1 -> past && r.status != ReservationStatus.CANCELLED
            else -> r.status == ReservationStatus.CANCELLED
        }
    }.sortedBy { it.reservation.checkIn }
    val heading = listOf("Upcoming reservations", "Past reservations", "Cancelled reservations")[tab]

    OwnerTabScaffold("Reservations", onNotifications, unread) {
        item { CortexSegmentedToggle(listOf("Upcoming", "Past", "Cancelled"), tab, { tab = it }, Modifier.padding(bottom = 4.dp)) }
        item { ResultsHeading(heading, "${visible.size} booking${if (visible.size == 1) "" else "s"}") }
        items(visible, key = { it.reservation.bookingId }) { item ->
            val r = item.reservation
            val site = campsite(r.campsiteId)
            ManagementCard(
                title = site?.name ?: "Removed campsite", subtitle = item.guestName, meta = nightsGuests(r.nights, r.guests),
                leading = { m -> CampsitePhoto(site, m) }, headerHeight = 88.dp,
                trailing = {
                    Text(r.status.label, style = MaterialTheme.typography.bodySmall,
                        color = if (r.status == ReservationStatus.CANCELLED) CortexTheme.colors.error else CortexPalette.BrandPrimary)
                },
            ) {
                Text(
                    formatRange(r.checkIn, r.checkOut),
                    style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                when {
                    r.status == ReservationStatus.PENDING ->
                        CortexButton("Reject reservation", { confirm = r.bookingId }, Modifier.fillMaxWidth(), style = CortexButtonStyle.Danger)
                    r.status == ReservationStatus.CONFIRMED && !r.checkOut.isBefore(today) -> ActionPair(
                        "Message user", { onMessage(item) },
                        "Cancel reservation", { confirm = r.bookingId },
                    )
                    else -> CortexButton("Message user", { onMessage(item) }, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand)
                }
            }
        }
    }
    confirm?.let { id ->
        val item = reservations.firstOrNull { it.reservation.bookingId == id }
        if (item == null) { confirm = null; return@let }
        val reject = item.reservation.status == ReservationStatus.PENDING
        CortexConfirmDialog(
            title = if (reject) "Reject reservation?" else "Cancel reservation?",
            text = "${item.guestName} will be notified and the dates will reopen for other campers.",
            confirmLabel = if (reject) "Reject" else "Cancel reservation", dismissLabel = "Keep", destructive = true,
            onConfirm = { onSetStatus(item, ReservationStatus.CANCELLED); confirm = null },
            onDismiss = { confirm = null },
        )
    }
}

/** Figma frame "Lotteries" in Owner */
@Composable
fun OwnerLotteriesScreen(
    lotteries: List<Lottery>,
    campsite: (String) -> Campsite?,
    entrantName: (String) -> String,
    unread: Int,
    onNotifications: () -> Unit,
    onCreate: () -> Unit,
    onUpdate: (Lottery) -> Unit,
    onNotifyWaitlist: (Lottery) -> Unit,
    onRunDraw: (Lottery) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var waitlistFor by rememberSaveable { mutableStateOf<String?>(null) }
    var drawFor by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = if (tab == 0) lotteries else lotteries.filter { it.status == LotteryStatus.ONGOING }

    OwnerTabScaffold("Lotteries", onNotifications, unread) {
        item { CortexSegmentedToggle(listOf("All lotteries", "Ongoing"), tab, { tab = it }, Modifier.padding(bottom = 4.dp)) }
        item { HeadingWithAction("Your lotteries · ${visible.size}", "Create lottery", onCreate) }
        if (visible.isEmpty()) item {
            Text("No lotteries yet.", style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary)
        }
        items(visible, key = { it.id }) { l ->
            val site = l.campsiteIds.firstOrNull()?.let(campsite)
            val ongoing = l.status == LotteryStatus.ONGOING
            ManagementCard(
                title = l.name.ifBlank { site?.name ?: "Lottery" },
                subtitle = l.campsiteIds.mapNotNull { campsite(it)?.name }.joinToString(", "),
                meta = "${l.entries.size}/${l.capacity} entered · " + if (l.hasWaitlist) "${l.waitlist.size}/${l.waitlistCapacity} waitlisted" else "Waitlist off",
                leading = { m -> CampsitePhoto(site, m) }, headerHeight = 88.dp,
                trailing = { Text(l.status.label, style = MaterialTheme.typography.bodySmall, color = CortexPalette.BrandPrimary) },
            ) {
                Text(
                    (if (ongoing) "Draw date: " else "Drawn: ") + formatDate(l.drawAt.toLocalDate()),
                    style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CortexButton("Update lottery", { onUpdate(l) }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                    if (ongoing && l.hasWaitlist) CortexButton("View waitlist", { waitlistFor = l.id }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                    else Box(Modifier.weight(1f))
                }
                if (ongoing && LocalDateTime.now().isAfter(l.closesAt)) {
                    CortexButton("Run draw", { drawFor = l.id }, Modifier.fillMaxWidth())
                }
                if (ongoing && l.hasWaitlist) {
                    CortexButton(
                        "Notify all waitlisted users", { onNotifyWaitlist(l) }, Modifier.fillMaxWidth(),
                        style = CortexButtonStyle.OutlinedBrand, leadingIcon = Icons.Outlined.Notifications,
                    )
                }
            }
        }
    }
    waitlistFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { waitlistFor = null; return@let }
        InfoDialog("Waitlist · ${l.waitlist.size}/${l.waitlistCapacity}", { waitlistFor = null }) {
            if (l.waitlist.isEmpty()) {
                Text("Nobody is on the waitlist yet.", style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary)
            }
            l.waitlist.forEachIndexed { i, entry ->
                Text("${i + 1}. ${entrantName(entry.userId)}", style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textPrimary)
            }
        }
    }
    drawFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { drawFor = null; return@let }
        CortexConfirmDialog(
            title = "Run the draw?",
            text = "${minOf(l.winners, l.entries.size)} of ${l.entries.size} entries will be picked at random and everyone will be notified.",
            confirmLabel = "Run draw",
            onConfirm = { onRunDraw(l); drawFor = null },
            onDismiss = { drawFor = null },
        )
    }
}

/** Figma frames "Owner Notifications - Received" and "Owner Notifications - Sent" */
@Composable
fun OwnerNotificationsScreen(
    received: List<AppNotification>,
    sent: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDownloadCsv: () -> Unit,
    onClick: (AppNotification) -> Unit,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val unread = received.count { !it.read }
    NotificationFeedScreen(
        notifications = if (tab == 0) received else sent,
        onBack = onBack,
        summary = if (tab == 0) "$unread unread · ${received.size} updates" else "${sent.size} sent notifications",
        showDot = tab == 1 || unread > 0,
        onNotificationClick = onClick,
        header = { CortexSegmentedToggle(listOf("Received", "Sent"), tab, { tab = it }) },
        controls = {
            if (tab == 0) CortexButton("Mark all read", onMarkAllRead, style = CortexButtonStyle.Text, size = CortexButtonSize.XSmall, enabled = unread > 0, touchTarget = 32.dp)
            else CortexButton("Download CSV", onDownloadCsv, style = CortexButtonStyle.Text, size = CortexButtonSize.XSmall, touchTarget = 32.dp)
        },
    )
}
