package com.example.cortex_camping.model

import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// lottery times are Edmonton times, matching TIME_ZONE in functions/lib/draw.js
val LOTTERY_TIME_ZONE: ZoneId = ZoneId.of("America/Edmonton")

// true while at least one night before [stayUntil] is still ahead, so a winner can still book
fun hasStayDatesLeft(stayUntil: LocalDate, today: LocalDate = LocalDate.now(LOTTERY_TIME_ZONE)): Boolean =
    !today.plusDays(1).isAfter(stayUntil)

// waitlistCapacity value for a waitlist that anyone can join
const val NO_WAITLIST_LIMIT = -1

enum class LotteryStatus(val label: String) {
    ONGOING("Ongoing"),
    FINALIZED("Finalized"), // the draw has happened
    CANCELLED("Cancelled") // the creator pulled out before the draw
}

// how a lottery turned out for one entrant
enum class LotteryOutcome(val label: String) {
    ENTERED("Entered"), // still waiting for the draw
    WON("Won"),
    LOST("Lost"),
    CANCELLED("Cancelled") // the lottery was cancelled before the draw
}

// one entrant's spot in a lottery (or its waitlist). campsiteId is the lottery's featured
// campsite they entered through: a winner can still book any campsite in the lottery
data class LotteryEntry(
    val userId: String = "",
    val campsiteId: String = ""
)

data class Lottery(
    val id: String = "",
    val ownerId: String = "", // the owner who created the lottery and controls its settings
    val ownerIds: List<String> = emptyList(), // every owner listing campsites in it, the creator included
    val invitedOwnerIds: List<String> = emptyList(), // owners who were invited and haven't answered yet
    val name: String = "",
    val campsiteIds: List<String> = emptyList(), // a lottery can cover campsites from several owners
    val featuredCampsiteId: String = "", // the creator's campsite campers enter through
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
    val nights: Int = 2, // length of the stay a winner gets
    val claimedIds: List<String> = emptyList(), // winners who picked a campsite and dates
    val declinedIds: List<String> = emptyList() // winners who declined the campsite after winning
) {
    // lotteries saved before ownerIds existed only have ownerId
    @get:Exclude
    val allOwnerIds: List<String>
        get() = (listOf(ownerId) + ownerIds).filter { it.isNotBlank() }.distinct()

    // the only campsite campers can enter through. lotteries saved before featured campsites
    // existed use their first campsite
    @get:Exclude
    val entryCampsiteId: String
        get() = featuredCampsiteId.ifBlank { campsiteIds.firstOrNull().orEmpty() }

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
            entryCampsiteId !in campsiteIds -> "Choose a featured campsite."
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

    fun isCreator(ownerId: String): Boolean = ownerId == this.ownerId

    fun isOwner(ownerId: String): Boolean = ownerId in allOwnerIds

    fun isInvited(ownerId: String): Boolean = ownerId in invitedOwnerIds

    /**
     * Swaps one owner's campsites for [chosen] and leaves every other owner's campsites alone.
     * [owned] is every campsite that owner has, so anything in [chosen] outside it is ignored.
     */
    fun withOwnerCampsites(owned: Set<String>, chosen: Collection<String>): Lottery = copy(
        campsiteIds = campsiteIds.filter { it !in owned || it in chosen } +
            chosen.filter { it in owned && it !in campsiteIds }.distinct()
    )

    // puts the creator's edits on top of the live lottery, so anyone who joined while the form
    // was open stays in, and so do the other owners and their campsites. [editorCampsites] is every
    // campsite the creator owns. if the capacity went up, the waitlist moves into the new slots.
    // if the featured campsite changed, everyone's entry moves to the new one
    fun withSettingsFrom(
        edited: Lottery,
        editorCampsites: Set<String> = campsiteIds.toSet() + edited.campsiteIds
    ): Lottery {
        val freeSlots = (edited.capacity - entries.size).coerceAtLeast(0)
        val entryCampsite = edited.entryCampsiteId
        fun List<LotteryEntry>.movedToEntry() = map { it.copy(campsiteId = entryCampsite) }
        return edited.copy(
            id = id,
            ownerId = ownerId,
            ownerIds = ownerIds,
            invitedOwnerIds = invitedOwnerIds,
            campsiteIds = withOwnerCampsites(editorCampsites, edited.campsiteIds).campsiteIds,
            featuredCampsiteId = entryCampsite,
            status = status,
            entries = (entries + waitlist.take(freeSlots)).movedToEntry(),
            waitlist = waitlist.drop(freeSlots).movedToEntry(),
            winnerIds = winnerIds
        )
    }

    fun canRunDraw(now: LocalDateTime = LocalDateTime.now(LOTTERY_TIME_ZONE)): Boolean =
        status == LotteryStatus.ONGOING && now.isAfter(closesAt)

    fun entryFor(userId: String): LotteryEntry? = entries.firstOrNull { it.userId == userId }

    fun isWaitlisted(userId: String): Boolean = waitlist.any { it.userId == userId }

    // 1 for the first person on the waitlist, 0 if they're not on it
    fun waitlistPosition(userId: String): Int = waitlist.indexOfFirst { it.userId == userId } + 1

    fun hasWon(userId: String): Boolean = userId in winnerIds

    // true while a winner still has to either book dates or decline
    fun hasOpenOffer(userId: String): Boolean = hasWon(userId) && userId !in claimedIds && userId !in declinedIds

    // every check-in date where a full stay fits inside the stay window and none of its nights are
    // taken. [taken] is a set of nights (a stay from the 5th to the 7th uses the nights of the 5th and 6th)
    // [openFrom] and [openUntil] are the campsite's own availability window, which can be narrower than the stay window

    fun availableCheckIns(
        taken: Set<LocalDate>,
        openFrom: LocalDate = LocalDate.MIN,
        openUntil: LocalDate = LocalDate.MAX
    ): List<LocalDate> {
        val first = if (openFrom.isAfter(stayFrom)) openFrom else stayFrom
        val last = if (openUntil.isBefore(stayUntil)) openUntil else stayUntil
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.plusDays(nights.toLong()).isAfter(last) }
            .filter { start -> (0 until nights).none { start.plusDays(it.toLong()) in taken } }
            .toList()
    }

    fun outcomeFor(userId: String): LotteryOutcome = when {
        status == LotteryStatus.CANCELLED -> LotteryOutcome.CANCELLED
        hasWon(userId) -> LotteryOutcome.WON
        status == LotteryStatus.FINALIZED -> LotteryOutcome.LOST
        else -> LotteryOutcome.ENTERED
    }
}
