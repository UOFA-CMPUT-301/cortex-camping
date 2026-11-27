package com.example.cortex_camping.ui.screens.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.ui.components.ActionPair
import com.example.cortex_camping.ui.components.AmenityChip
import com.example.cortex_camping.ui.components.AvailabilityDialog
import com.example.cortex_camping.ui.components.BellAction
import com.example.cortex_camping.ui.components.CortexBackTopBar
import com.example.cortex_camping.ui.components.CortexButton
import com.example.cortex_camping.ui.components.CortexButtonSize
import com.example.cortex_camping.ui.components.CortexButtonStyle
import com.example.cortex_camping.ui.components.CortexConfirmDialog
import com.example.cortex_camping.ui.components.CortexContentDialog
import com.example.cortex_camping.ui.components.CortexTextField
import com.example.cortex_camping.ui.components.MultiSelectDialog
import com.example.cortex_camping.ui.components.CortexDivider
import com.example.cortex_camping.ui.components.CortexMediumTopBar
import com.example.cortex_camping.ui.components.CortexSegmentedToggle
import com.example.cortex_camping.ui.components.CampsitePhoto
import com.example.cortex_camping.ui.components.GroupedSurface
import com.example.cortex_camping.ui.components.InfoDialog
import com.example.cortex_camping.ui.components.LinkListItem
import com.example.cortex_camping.ui.components.LocationMapCard
import com.example.cortex_camping.ui.components.formatDate
import com.example.cortex_camping.ui.components.formatRange
import com.example.cortex_camping.ui.components.formatRating
import com.example.cortex_camping.ui.components.ManagementCard
import com.example.cortex_camping.ui.components.QrCodeDialog
import com.example.cortex_camping.ui.components.ResultsHeading
import com.example.cortex_camping.ui.components.RoleSwitchPill
import com.example.cortex_camping.ui.components.SectionTitle
import com.example.cortex_camping.ui.components.nightsGuests
import com.example.cortex_camping.ui.screens.NotificationFeedScreen
import com.example.cortex_camping.ui.theme.CortexPalette
import com.example.cortex_camping.ui.theme.CortexTheme
import com.example.cortex_camping.viewmodel.Role
import java.time.LocalDate

data class GuestReservation(val reservation: Reservation, val guestName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerTabScaffold(
    title: String,
    currentRole: Role = Role.Owner,
    onRoleSwitch: (Role) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotifications: () -> Unit,
    unread: Int,
    content: LazyListContent,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            CortexMediumTopBar(
                title,
                navigationIcon = {
                    RoleSwitchPill(currentRole = currentRole, onSwitchRole = onRoleSwitch)
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            Icons.Outlined.AccountCircle,
                            contentDescription = "Profile and settings",
                            tint = CortexTheme.colors.textPrimary
                        )
                    }
                    BellAction(onNotifications, unread)
                },
                scrollBehavior = scroll,
            )
        },
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
    currentRole: Role,
    onRoleSwitch: (Role) -> Unit,
    onProfileClick: () -> Unit,
    onNotifications: () -> Unit,
    onAdd: () -> Unit,
    onView: (Campsite) -> Unit,
    onEdit: (Campsite) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var qrFor by rememberSaveable { mutableStateOf<String?>(null) }
    var datesFor by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = if (tab == 0) campsites else campsites.filter(hasOngoingLottery)

    OwnerTabScaffold("My Campsites", currentRole, onRoleSwitch, onProfileClick, onNotifications, unread) {
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
                CortexButton("View details", { onView(site) }, Modifier.fillMaxWidth(), touchTarget = 40.dp)
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

/** What the owner sees for one of their campsites, everything on it is read-only until they hit edit */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerCampsiteDetailsScreen(
    campsite: Campsite,
    bookedDates: Set<LocalDate>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenRules: () -> Unit,
) {
    val colors = CortexTheme.colors
    val availability = campsite.availability
    var showDates by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { CortexBackTopBar("Campsite details", onBack) },
        containerColor = colors.surfaceBase,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CampsitePhoto(campsite, Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(campsite.name, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                Text(
                    "Max ${campsite.maxOccupancy} guests · $%.2f CAD per night".format(campsite.price),
                    style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                )
                Text(
                    if (campsite.ratingCount == 0) "★ No ratings yet" else "★ ${formatRating(campsite)} · ${campsite.ratingCount} ratings",
                    style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                )
            }
            Text(
                campsite.description.ifBlank { "No description added yet." },
                style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary,
            )
            if (campsite.tag.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { campsite.tag.forEach { AmenityChip(it) } }
            }
            LocationMapCard(campsite.location, onOpenMap)
            GroupedSurface {
                LinkListItem(
                    "Availability", "${formatRange(availability.availableFrom, availability.availableUntil)} · ${availability.unavailableDates.size} blocked",
                    { showDates = true },
                )
                CortexDivider()
                LinkListItem("Rules & policies", campsite.rulesUrl.ifBlank { "No rules added yet" }, onOpenRules)
            }
            CortexButton("Edit campsite", onEdit, Modifier.fillMaxWidth())
        }
    }
    if (showDates) {
        AvailabilityDialog("${campsite.name} · booked dates", bookedDates, editable = false, onDismiss = { showDates = false })
    }
}

/** Figma frame "Reservations" in Owner */
@Composable
fun OwnerReservationsScreen(
    reservations: List<GuestReservation>,
    campsite: (String) -> Campsite?,
    unread: Int,
    currentRole: Role,
    onRoleSwitch: (Role) -> Unit,
    onProfileClick: () -> Unit,
    onNotifications: () -> Unit,
    onMessage: (GuestReservation) -> Unit,
    onReject: (GuestReservation, String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var rejecting by rememberSaveable { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val visible = reservations.filter { (r) ->
        val past = r.checkOut.isBefore(today)
        when (tab) {
            0 -> !past && !r.isWithdrawn
            1 -> past && !r.isWithdrawn
            else -> r.isWithdrawn
        }
    }.sortedBy { it.reservation.checkIn }
    val heading = listOf("Upcoming reservations", "Past reservations", "Rejected & cancelled")[tab]

    OwnerTabScaffold("Reservations", currentRole, onRoleSwitch, onProfileClick, onNotifications, unread) {
        item { CortexSegmentedToggle(listOf("Upcoming", "Past", "Closed"), tab, { tab = it }, Modifier.padding(bottom = 4.dp)) }
        item { ResultsHeading(heading, "${visible.size} booking${if (visible.size == 1) "" else "s"}") }
        if (visible.isEmpty()) item {
            Text(
                when (tab) {
                    0 -> "No upcoming reservations for your campsites yet."
                    1 -> "No past reservations for your campsites yet."
                    else -> "No rejected or cancelled reservations for your campsites."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = CortexTheme.colors.textSecondary,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        items(visible, key = { it.reservation.bookingId }) { item ->
            val r = item.reservation
            val site = campsite(r.campsiteId)
            ManagementCard(
                title = site?.name ?: "Removed campsite", subtitle = item.guestName, meta = nightsGuests(r.nights, r.guests),
                leading = { m -> CampsitePhoto(site, m) }, headerHeight = 88.dp,
                trailing = {
                    Text(r.status.label, style = MaterialTheme.typography.bodySmall,
                        color = if (r.isWithdrawn) CortexTheme.colors.error else CortexPalette.BrandPrimary)
                },
            ) {
                Text(
                    formatRange(r.checkIn, r.checkOut),
                    style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                if (r.status == ReservationStatus.REJECTED && r.rejectionReason.isNotBlank()) {
                    Text(
                        "Reason: ${r.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!r.isWithdrawn && !r.checkOut.isBefore(today)) {
                    ActionPair("Message user", { onMessage(item) }, "Reject reservation", { rejecting = r.bookingId })
                } else {
                    CortexButton("Message user", { onMessage(item) }, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand)
                }
            }
        }
    }
    rejecting?.let { id ->
        val item = reservations.firstOrNull { it.reservation.bookingId == id }
        if (item == null) { rejecting = null; return@let }
        RejectReservationDialog(
            guestName = item.guestName,
            fromLottery = item.reservation.lotteryId.isNotBlank(),
            onReject = { reason -> onReject(item, reason); rejecting = null },
            onDismiss = { rejecting = null },
        )
    }
}

@Composable
private fun RejectReservationDialog(guestName: String, fromLottery: Boolean, onReject: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    CortexContentDialog(
        title = "Reject reservation?",
        onDismiss = onDismiss,
        actions = {
            Box(Modifier.weight(1f))
            CortexButton("Keep", onDismiss, style = CortexButtonStyle.Text)
            CortexButton("Reject", { onReject(reason.trim()) }, style = CortexButtonStyle.TextDanger)
        },
    ) {
        Text(
            "$guestName will be notified and the dates will reopen for other campers." +
                if (fromLottery) " Their lottery win opens again so they can choose new dates, unless the stay period is over." else "",
            style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        CortexTextField(reason, { reason = it }, "Reason (optional)", singleLine = false, imeAction = ImeAction.Default, placeholder = "Tell the camper why")
    }
}

/** What an owner can do with a lottery shared between several owners. */
data class SharedLotteryActions(
    val invite: (Lottery, String) -> Unit,
    val respond: (Lottery, Boolean) -> Unit,
    val setMyCampsites: (Lottery, Set<String>) -> Unit,
    val removeOwner: (Lottery, String) -> Unit,
)

/** Figma frame "Lotteries" in Owner */
@Composable
fun OwnerLotteriesScreen(
    lotteries: List<Lottery>,
    invites: List<Lottery>,
    userId: String,
    myCampsites: List<Campsite>,
    campsite: (String) -> Campsite?,
    entrantName: (String) -> String,
    ownerName: (String) -> String,
    unread: Int,
    currentRole: Role,
    onRoleSwitch: (Role) -> Unit,
    onProfileClick: () -> Unit,
    onNotifications: () -> Unit,
    onCreate: () -> Unit,
    onUpdate: (Lottery) -> Unit,
    onNotifyWaitlist: (Lottery) -> Unit,
    onRunDraw: (Lottery) -> Unit,
    onCancel: (Lottery) -> Unit,
    shared: SharedLotteryActions,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var waitlistFor by rememberSaveable { mutableStateOf<String?>(null) }
    var drawFor by rememberSaveable { mutableStateOf<String?>(null) }
    var inviteFor by rememberSaveable { mutableStateOf<String?>(null) }
    var ownersFor by rememberSaveable { mutableStateOf<String?>(null) }
    var campsitesFor by rememberSaveable { mutableStateOf<String?>(null) }
    var leaveFor by rememberSaveable { mutableStateOf<String?>(null) }
    var cancelFor by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = if (tab == 0) lotteries else lotteries.filter { it.status == LotteryStatus.ONGOING }
    val myIds = myCampsites.map { it.campsiteId }.toSet()

    OwnerTabScaffold("Lotteries", currentRole, onRoleSwitch, onProfileClick, onNotifications, unread) {
        if (invites.isNotEmpty()) {
            item { SectionTitle("Invites · ${invites.size}") }
            items(invites, key = { "invite-${it.id}" }) { l ->
                val site = l.campsiteIds.firstOrNull()?.let(campsite)
                ManagementCard(
                    title = l.name.ifBlank { "Lottery" },
                    subtitle = "Invited by ${ownerName(l.ownerId)}",
                    meta = "Stays ${formatRange(l.stayFrom, l.stayUntil)}",
                    leading = { m -> CampsitePhoto(site, m) }, headerHeight = 88.dp,
                ) {
                    Text(
                        "Accept to list your own campsites in this lottery.",
                        style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CortexButton("Decline", { shared.respond(l, false) }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                        CortexButton("Accept", { shared.respond(l, true) }, Modifier.weight(1f))
                    }
                }
            }
        }
        item { CortexSegmentedToggle(listOf("All lotteries", "Ongoing"), tab, { tab = it }, Modifier.padding(bottom = 4.dp)) }
        item { HeadingWithAction("Your lotteries · ${visible.size}", "Create lottery", onCreate) }
        if (visible.isEmpty()) item {
            Text("No lotteries yet.", style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary)
        }
        items(visible, key = { it.id }) { l ->
            val creator = l.isCreator(userId)
            val site = (l.campsiteIds.firstOrNull { it in myIds } ?: l.campsiteIds.firstOrNull())?.let(campsite)
            val ongoing = l.status == LotteryStatus.ONGOING
            val cancelled = l.status == LotteryStatus.CANCELLED
            val others = l.allOwnerIds.size - 1
            ManagementCard(
                title = l.name.ifBlank { site?.name ?: "Lottery" },
                subtitle = l.campsiteIds.mapNotNull { campsite(it)?.name }.joinToString(", "),
                meta = "${l.entries.size}/${l.capacity} entered · " + if (l.hasWaitlist) "${l.waitlistSummary} waitlisted" else "Waitlist off",
                leading = { m -> CampsitePhoto(site, m) }, headerHeight = 88.dp,
                trailing = {
                    Text(l.status.label, style = MaterialTheme.typography.bodySmall,
                        color = if (cancelled) CortexTheme.colors.error else CortexPalette.BrandPrimary)
                },
            ) {
                Text(
                    when {
                        cancelled -> "Cancelled before the draw"
                        ongoing -> "Draw date: " + formatDate(l.drawAt.toLocalDate())
                        else -> "Drawn: " + formatDate(l.drawAt.toLocalDate())
                    },
                    style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                Text(
                    "Featured campsite: ${campsite(l.entryCampsiteId)?.name ?: "Removed campsite"}",
                    style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    when {
                        !creator -> "Shared by ${ownerName(l.ownerId)}"
                        others > 0 -> "Shared with $others other owner${if (others == 1) "" else "s"}"
                        else -> "Only your campsites"
                    } + if (creator && l.invitedOwnerIds.isNotEmpty()) " · ${l.invitedOwnerIds.size} invited" else "",
                    style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                if (creator) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CortexButton("Update lottery", { onUpdate(l) }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand, enabled = ongoing)
                        if (ongoing && l.hasWaitlist) CortexButton("View waitlist", { waitlistFor = l.id }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                        else Box(Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!cancelled) CortexButton("Invite owner", { inviteFor = l.id }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                        CortexButton("Owners", { ownersFor = l.id }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                    }
                    if (l.canRunDraw()) {
                        CortexButton("Run draw", { drawFor = l.id }, Modifier.fillMaxWidth())
                    }
                    if (ongoing && l.hasWaitlist) {
                        CortexButton(
                            "Notify all waitlisted users", { onNotifyWaitlist(l) }, Modifier.fillMaxWidth(),
                            style = CortexButtonStyle.OutlinedBrand, leadingIcon = Icons.Outlined.Notifications,
                        )
                    }
                    if (ongoing) {
                        CortexButton("Cancel lottery", { cancelFor = l.id }, Modifier.fillMaxWidth(), style = CortexButtonStyle.DangerSubtle)
                    }
                } else if (cancelled) {
                    CortexButton("Owners", { ownersFor = l.id }, Modifier.fillMaxWidth(), style = CortexButtonStyle.OutlinedBrand)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CortexButton("Manage my campsites", { campsitesFor = l.id }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                        CortexButton("Owners", { ownersFor = l.id }, Modifier.weight(1f), style = CortexButtonStyle.OutlinedBrand)
                    }
                    CortexButton("Leave lottery", { leaveFor = l.id }, Modifier.fillMaxWidth(), style = CortexButtonStyle.DangerSubtle)
                }
            }
        }
    }
    waitlistFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { waitlistFor = null; return@let }
        InfoDialog("Waitlist · ${l.waitlistSummary}", { waitlistFor = null }) {
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
    inviteFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { inviteFor = null; return@let }
        InviteOwnerDialog(l.name, onInvite = { shared.invite(l, it); inviteFor = null }, onDismiss = { inviteFor = null })
    }
    ownersFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { ownersFor = null; return@let }
        val canManage = l.isCreator(userId) && l.status != LotteryStatus.CANCELLED
        InfoDialog("Owners", { ownersFor = null }) {
            l.allOwnerIds.forEach { ownerId ->
                OwnerRow(
                    name = ownerName(ownerId) + if (l.isCreator(ownerId)) " (creator)" else "",
                    detail = l.campsiteIds.mapNotNull { campsite(it) }.filter { it.ownerId == ownerId }
                        .joinToString(", ") { it.name }.ifBlank { "No campsites listed" },
                    action = if (canManage && !l.isCreator(ownerId)) "Remove" else null,
                    onAction = { shared.removeOwner(l, ownerId) },
                )
            }
            l.invitedOwnerIds.forEach { ownerId ->
                OwnerRow(
                    name = ownerName(ownerId), detail = "Invite pending",
                    action = if (canManage) "Cancel invite" else null,
                    onAction = { shared.removeOwner(l, ownerId) },
                )
            }
        }
    }
    campsitesFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { campsitesFor = null; return@let }
        if (myCampsites.isEmpty()) {
            InfoDialog("My campsites", { campsitesFor = null }) {
                Text("Add a campsite first, then you can list it in this lottery.", style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary)
            }
        } else {
            MultiSelectDialog(
                "My campsites", myCampsites.map { it.campsiteId to it.name }, l.campsiteIds.filter { it in myIds }.toSet(),
                onConfirm = { shared.setMyCampsites(l, it); campsitesFor = null }, onDismiss = { campsitesFor = null },
            )
        }
    }
    cancelFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { cancelFor = null; return@let }
        val shared = l.allOwnerIds.size > 1
        CortexConfirmDialog(
            title = "Cancel this lottery?",
            text = "${l.name} won't be drawn. Everyone who entered" +
                (if (shared) " and every owner sharing it" else "") + " will be notified. This can't be undone.",
            confirmLabel = "Cancel lottery", dismissLabel = "Keep", destructive = true,
            onConfirm = { onCancel(l); cancelFor = null },
            onDismiss = { cancelFor = null },
        )
    }
    leaveFor?.let { id ->
        val l = lotteries.firstOrNull { it.id == id }
        if (l == null) { leaveFor = null; return@let }
        CortexConfirmDialog(
            title = "Leave this lottery?",
            text = "Your campsites will be taken out of ${l.name}.",
            confirmLabel = "Leave", destructive = true,
            onConfirm = { shared.removeOwner(l, userId); leaveFor = null },
            onDismiss = { leaveFor = null },
        )
    }
}

@Composable
private fun OwnerRow(name: String, detail: String, action: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = CortexTheme.colors.textSecondary)
        }
        if (action != null) {
            CortexButton(action, onAction, style = CortexButtonStyle.TextDanger, size = CortexButtonSize.XSmall, touchTarget = 32.dp)
        }
    }
}

@Composable
private fun InviteOwnerDialog(lotteryName: String, onInvite: (String) -> Unit, onDismiss: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    CortexContentDialog(
        title = "Invite an owner",
        onDismiss = onDismiss,
        actions = {
            Box(Modifier.weight(1f))
            CortexButton("Cancel", onDismiss, style = CortexButtonStyle.Text)
            CortexButton("Send invite", { onInvite(email.trim()) }, style = CortexButtonStyle.Text, enabled = email.isNotBlank())
        },
    ) {
        Text(
            "They'll be able to list their own campsites in $lotteryName once they accept.",
            style = MaterialTheme.typography.bodyMedium, color = CortexTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        CortexTextField(email, { email = it }, "Owner's registered email", keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, placeholder = "name@example.com")
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
