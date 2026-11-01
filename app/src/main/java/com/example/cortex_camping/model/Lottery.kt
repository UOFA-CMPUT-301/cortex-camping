package com.example.cortex_camping.model

import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.LocalDateTime

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
    val waitlistCapacity: Int = 0, // 0 means the waitlist is off
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
        get() = waitlistCapacity > 0

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
