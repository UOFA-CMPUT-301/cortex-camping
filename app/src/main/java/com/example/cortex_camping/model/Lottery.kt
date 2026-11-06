package com.example.cortex_camping.model

import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// waitlistCapacity value for a waitlist that anyone can join
const val NO_WAITLIST_LIMIT = -1

enum class LotteryStatus(val label: String) {
    ONGOING("Ongoing"),
    FINALIZED("Finalized") // the draw has happened
}

// how a lottery turned out for one entrant
enum class LotteryOutcome(val label: String) {
    ENTERED("Entered"), // still waiting for the draw
    WON("Won"),
    LOST("Lost")
}

// one entrant's spot in a lottery (or its waitlist), tied to the campsite they entered from
data class LotteryEntry(
    val userId: String = "",
    val campsiteId: String = ""
)

data class Lottery(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val campsiteIds: List<String> = emptyList(), // a lottery can cover several of the owner's campsites
    val status: LotteryStatus = LotteryStatus.ONGOING,
    val capacity: Int = 50, // max number of entries in the draw
    val winners: Int = 3, // how many entries get picked
    val waitlistCapacity: Int = 0, // 0 means the waitlist is off, NO_WAITLIST_LIMIT means it has no cap
    val entries: List<LotteryEntry> = emptyList(),
    val waitlist: List<LotteryEntry> = emptyList(), // in order, first in line is first
    val winnerIds: List<String> = emptyList(),
    val opensAt: LocalDateTime = LocalDateTime.now(),
    val closesAt: LocalDateTime = LocalDateTime.now().plusDays(1),
    val drawAt: LocalDateTime = LocalDateTime.now().plusDays(2),
    val stayFrom: LocalDate = LocalDate.now().plusMonths(1), // winners pick their dates inside this window
    val stayUntil: LocalDate = LocalDate.now().plusMonths(2),
    val nights: Int = 2 // length of the stay a winner gets
) {
    @get:Exclude
    val isFull: Boolean
        get() = entries.size >= capacity

    @get:Exclude
    val hasWaitlist: Boolean
        get() = waitlistCapacity != 0

    @get:Exclude
    val hasWaitlistLimit: Boolean
        get() = waitlistCapacity != NO_WAITLIST_LIMIT

    // the most people the waitlist can hold
    @get:Exclude
    val waitlistLimit: Int
        get() = if (hasWaitlistLimit) waitlistCapacity else Int.MAX_VALUE

    // e.g. "3/20", or just "3" when there's no cap
    @get:Exclude
    val waitlistSummary: String
        get() = if (hasWaitlistLimit) "${waitlist.size}/$waitlistCapacity" else "${waitlist.size}"

    // what's stopping the owner from saving these settings, null if they're fine
    @get:Exclude
    val settingsError: String?
        get() = when {
            capacity < 1 -> "Lottery capacity must be at least 1."
            winners < 1 -> "A lottery needs at least 1 winner."
            winners > capacity -> "There can't be more winners than lottery entries."
            nights < 1 -> "Stays must be at least 1 night."
            !closesAt.isAfter(opensAt) -> "Entry has to close after it opens."
            drawAt.isBefore(closesAt) -> "The draw can't be before entry closes."
            !stayUntil.isAfter(stayFrom) -> "Stays need to end after they start."
            stayFrom.isBefore(drawAt.toLocalDate()) -> "Stays can't start before the draw."
            ChronoUnit.DAYS.between(stayFrom, stayUntil) < nights -> "The stay period is too short for $nights nights."
            else -> null
        }

    fun isRegistrationOpen(now: LocalDateTime = LocalDateTime.now()): Boolean =
        status == LotteryStatus.ONGOING && !now.isBefore(opensAt) && !now.isAfter(closesAt)

    // puts the owner's edits on top of the live lottery, so anyone who joined while the form
    // was open stays in. if the capacity went up, the waitlist moves into the new slots
    fun withSettingsFrom(edited: Lottery): Lottery {
        val freeSlots = (edited.capacity - entries.size).coerceAtLeast(0)
        return edited.copy(
            id = id,
            ownerId = ownerId,
            status = status,
            entries = entries + waitlist.take(freeSlots),
            waitlist = waitlist.drop(freeSlots),
            winnerIds = winnerIds
        )
    }

    fun entryFor(userId: String): LotteryEntry? = entries.firstOrNull { it.userId == userId }

    fun isWaitlisted(userId: String): Boolean = waitlist.any { it.userId == userId }

    // 1 for the first person on the waitlist, 0 if they're not on it
    fun waitlistPosition(userId: String): Int = waitlist.indexOfFirst { it.userId == userId } + 1

    fun hasWon(userId: String): Boolean = userId in winnerIds

    fun outcomeFor(userId: String): LotteryOutcome = when {
        hasWon(userId) -> LotteryOutcome.WON
        status == LotteryStatus.FINALIZED -> LotteryOutcome.LOST
        else -> LotteryOutcome.ENTERED
    }
}
