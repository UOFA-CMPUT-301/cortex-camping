package com.example.cortex_camping.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryOutcome
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.ui.theme.CortexTheme
import java.time.LocalDateTime

@Composable
private fun UpcomingCard(
    campsite: Campsite,
    meta: String,
    status: String,
    statusColor: Color,
    dates: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier,
) {
    val colors = CortexTheme.colors
    CortexOutlinedCard(modifier) {
        CampsiteSummaryHeader(campsite, campsite.location, meta, status, statusColor)
        CortexDivider()
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dates != null) {
                Text(dates, style = CortexTheme.emphasized.bodyMedium, color = colors.textPrimary)
            } else {
                Text("Dates not selected", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CortexButton(primaryLabel, onPrimary, Modifier.weight(1f))
                CortexButton(secondaryLabel, onSecondary, Modifier.weight(1f), style = CortexButtonStyle.Danger)
            }
        }
    }
}

@Composable
fun ReservationCard(
    reservation: Reservation,
    campsite: Campsite,
    onMessageOwner: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) = UpcomingCard(
    campsite = campsite,
    meta = nightsGuests(reservation.nights, reservation.guests),
    status = reservation.status.label,
    statusColor = if (reservation.status == ReservationStatus.CONFIRMED) MaterialTheme.colorScheme.primary else CortexTheme.colors.textSecondary,
    dates = formatStay(reservation.checkIn, reservation.checkOut),
    primaryLabel = "Message Owner", onPrimary = onMessageOwner,
    secondaryLabel = "Cancel", onSecondary = onCancel,
    modifier = modifier,
)

@Composable
fun BookingOfferCard(
    lottery: Lottery,
    campsite: Campsite,
    onChooseDates: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) = UpcomingCard(
    campsite = campsite,
    meta = nights(lottery.nights),
    status = "Unconfirmed",
    statusColor = CortexTheme.colors.textSecondary,
    dates = null,
    primaryLabel = "Choose dates", onPrimary = onChooseDates,
    secondaryLabel = "Decline", onSecondary = onDecline,
    modifier = modifier,
)

@Composable
fun PastReservationCard(
    reservation: Reservation,
    campsite: Campsite,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    CortexOutlinedCard(modifier) {
        CampsiteSummaryHeader(campsite, campsite.location, nightsGuests(reservation.nights, reservation.guests))
        CortexDivider()
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)) {
            Text(formatStay(reservation.checkIn, reservation.checkOut), style = CortexTheme.emphasized.bodyMedium, color = CortexTheme.colors.textPrimary)
            StarRatingRow(rating = reservation.rating, onRate = onRate, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun LotteryEntryCard(
    lottery: Lottery,
    campsite: Campsite,
    userId: String,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors
    val outcome = lottery.outcomeFor(userId)
    val drawn = lottery.status == LotteryStatus.FINALIZED
    val detail = when (outcome) {
        LotteryOutcome.ENTERED -> "Lottery slot confirmed. Included in the draw."
        LotteryOutcome.WON -> "You won a spot! Your entry was selected."
        LotteryOutcome.LOST -> "Not selected in this draw. Try again next time."
    }
    CortexOutlinedCard(modifier) {
        CampsiteSummaryHeader(
            campsite, campsite.location, nights(lottery.nights),
            status = outcome.label,
            statusColor = if (outcome == LotteryOutcome.LOST) colors.textPrimary else MaterialTheme.colorScheme.primary,
        )
        CortexDivider()
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                (if (drawn) "Drawn: " else "Draw date: ") + formatDate(lottery.drawAt.toLocalDate()),
                style = CortexTheme.emphasized.bodyMedium, color = colors.textPrimary,
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            if (outcome == LotteryOutcome.ENTERED) {
                CortexButton("Leave lottery", onLeave, Modifier.fillMaxWidth(), style = CortexButtonStyle.DangerSubtle)
            }
        }
    }
}

@Composable
fun CampsiteLotteryCard(
    lottery: Lottery,
    userId: String,
    onEnter: () -> Unit,
    onLeave: () -> Unit,
    onJoinWaitlist: () -> Unit,
    onLeaveWaitlist: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CortexTheme.colors
    val waitlistText = if (lottery.hasWaitlist) "Waitlist: ${lottery.waitlistSummary}" else "Waitlist off"
    val slotsLeft = lottery.capacity - lottery.entries.size
    val waitlistLeft = lottery.waitlistLimit - lottery.waitlist.size
    val drawDate = formatDate(lottery.drawAt.toLocalDate())
    val open = lottery.isRegistrationOpen()
    val opensLater = lottery.status == LotteryStatus.ONGOING && LocalDateTime.now().isBefore(lottery.opensAt)

    data class Ui(val title: String, val body: String, val link: String, val action: @Composable () -> Unit)

    val ui = when {
        lottery.entryFor(userId) != null -> Ui(
            "You are entered", "You are included in the draw on $drawDate.", "Manage entry ›",
        ) { CortexButton("Leave lottery", onLeave, style = CortexButtonStyle.Danger, size = CortexButtonSize.XSmall, enabled = open) }

        lottery.isWaitlisted(userId) -> Ui(
            "Waitlisted · position ${lottery.waitlistPosition(userId)}", "Not entered in the draw. Waiting for a lottery slot.", "Manage entry ›",
        ) { CortexButton("Leave waitlist", onLeaveWaitlist, style = CortexButtonStyle.Danger, size = CortexButtonSize.XSmall) }

        opensLater -> Ui(
            "Registration not open yet", "Entries open ${formatDate(lottery.opensAt.toLocalDate())} at ${formatTime(lottery.opensAt).lowercase()}.", "Lottery details ›",
        ) { CortexButton("Not open yet", {}, enabled = false) }

        !open -> Ui(
            "Registration closed", "This lottery is no longer taking entries.", "Lottery details ›",
        ) { CortexButton("Closed", {}, enabled = false) }

        !lottery.isFull -> Ui(
            "You are not entered", "$slotsLeft entry slots available. Enter before registration closes.", "Lottery details ›",
        ) { CortexButton("Enter lottery", onEnter) }

        lottery.hasWaitlist && waitlistLeft > 0 -> Ui(
            if (lottery.hasWaitlistLimit) "Lottery full · $waitlistLeft waitlist places available" else "Lottery full · waitlist open", "Join the waitlist for a slot. You are not in the draw.", "Manage entry ›",
        ) { CortexButton("Join waitlist", onJoinWaitlist, size = CortexButtonSize.XSmall) }

        else -> Ui(
            "Lottery full", "No entry slots remain. This lottery has no waitlist.", "Lottery details ›",
        ) { CortexButton("Lottery full", {}, enabled = false) }
    }

    CortexOutlinedCard(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lottery.name, style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp), color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(if (open) "Open" else if (opensLater) "Upcoming" else "Closed")
                Text("Draw $drawDate", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Registration: ${formatRange(lottery.opensAt.toLocalDate(), lottery.closesAt.toLocalDate())} · closes ${formatTime(lottery.closesAt).lowercase()}",
                    style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                )
                Text(
                    "Stays: ${formatRange(lottery.stayFrom, lottery.stayUntil)} · ${lottery.winners} winners",
                    style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                )
                Text("Lottery entries: ${lottery.entries.size}/${lottery.capacity} · $waitlistText", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            InfoBox(ui.title, ui.body)
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InlineLink(ui.link, onDetails)
                ui.action()
            }
        }
    }
}
