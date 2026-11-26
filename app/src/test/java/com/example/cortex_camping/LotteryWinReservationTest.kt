package com.example.cortex_camping

import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryOutcome
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.repository.ReservationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for what a lottery winner can do: see open dates, pick a campsite in a
 * multi-campsite lottery, and decline.
 *
 * Goes over User Stories USER: US 01.04.01, 01.04.02, and 01.04.03
 *
 * Stay window used everywhere: Jul 1 to Jul 8 2027, 2 nights per stay,
 * so the last possible check-in is Jul 6
 */

class LotteryWinReservationTest {

    private fun day(d: Int) = LocalDate.of(2027, 7, d)

    private fun lottery(
        campsiteIds: List<String> = listOf("A"),
        winnerIds: List<String> = listOf("winner"),
        claimedIds: List<String> = emptyList(),
        declinedIds: List<String> = emptyList()
    ) = Lottery(
        id = "L1",
        ownerId = "owner",
        campsiteIds = campsiteIds,
        status = LotteryStatus.FINALIZED,
        winnerIds = winnerIds,
        stayFrom = day(1),
        stayUntil = day(8),
        nights = 2,
        claimedIds = claimedIds,
        declinedIds = declinedIds
    )

    // US 01.04.01, winning a lottery and seeing the available dates

    @Test
    fun availableCheckIns_noneTaken_listsEveryStartThatFitsTheWindow() {
        val dates = lottery().availableCheckIns(emptySet())
        assertEquals((1..6).map { day(it) }, dates)
    }

    @Test
    fun availableCheckIns_takenNightRemovesEveryStayThatUsesIt() {
        // the night of Jul 3 is taken, so stays starting Jul 2 (2,3) and Jul 3 (3,4) are gone
        val dates = lottery().availableCheckIns(setOf(day(3)))
        assertEquals(listOf(day(1), day(4), day(5), day(6)), dates)
    }

    @Test
    fun availableCheckIns_everyNightTaken_returnsNothing() {
        val allTaken = (1..7).map { day(it) }.toSet()
        assertTrue(lottery().availableCheckIns(allTaken).isEmpty())
    }

    @Test
    fun availableCheckIns_respectsTheCampsitesOwnOpenWindow() {
        // the campsite is only open Jul 4 to Jul 7, narrower than the lottery's window
        val dates = lottery().availableCheckIns(emptySet(), openFrom = day(4), openUntil = day(7))
        assertEquals(listOf(day(4), day(5)), dates)
    }

    // US 01.04.02, winning a multiple campsite lottery

    @Test
    fun multiCampsite_onlyCampsitesWithOpenDatesCanBePicked() {
        val lottery = lottery(campsiteIds = listOf("A", "B"))
        val takenByCampsite = mapOf(
            "A" to (1..7).map { day(it) }.toSet(), // A is fully booked
            "B" to emptySet<LocalDate>()
        )

        val pickable = lottery.campsiteIds.filter {
            lottery.availableCheckIns(takenByCampsite.getValue(it)).isNotEmpty()
        }

        assertEquals(listOf("B"), pickable)
    }

    @Test
    fun multiCampsite_reservationKeepsTheCampsiteChosenAndTheLottery() {
        val chosen = Reservation(
            bookingId = "booking1",
            campsiteId = "B", // the winner entered through A but chose B
            userId = "winner",
            lotteryId = "L1",
            checkIn = day(2),
            checkOut = day(4)
        )

        val saved = ReservationRepository.fromMap(ReservationRepository.toMap(chosen))

        assertEquals(chosen, saved)
        assertEquals("B", saved?.campsiteId)
        assertEquals("L1", saved?.lotteryId)
    }

    @Test
    fun reservation_overlappingStaysClash_backToBackStaysDoNot() {
        val first = Reservation(campsiteId = "A", checkIn = day(2), checkOut = day(4))
        val overlapping = Reservation(campsiteId = "A", checkIn = day(3), checkOut = day(5))
        val backToBack = Reservation(campsiteId = "A", checkIn = day(4), checkOut = day(6))

        assertTrue(first.overlapsWith(overlapping))
        assertFalse(first.overlapsWith(backToBack))
    }

    @Test
    fun reservation_unreadableDatesAreIgnoredInsteadOfCrashing() {
        val broken = mapOf<String, Any?>("bookingId" to "x", "checkIn" to "not a date")
        assertNull(ReservationRepository.fromMap(broken))
    }

    // US 01.04.03, winning a campsite lottery and declining the reservation

    @Test
    fun winner_hasAnOpenOffer() {
        assertTrue(lottery().hasOpenOffer("winner"))
    }

    @Test
    fun nonWinner_neverHasAnOffer() {
        assertFalse(lottery().hasOpenOffer("someoneElse"))
    }

    @Test
    fun declining_closesTheOffer() {
        val afterDecline = lottery(declinedIds = listOf("winner"))
        assertFalse(afterDecline.hasOpenOffer("winner"))
    }

    @Test
    fun declining_doesNotAffectOtherWinners() {
        val afterDecline = lottery(
            winnerIds = listOf("winner", "other"),
            declinedIds = listOf("winner")
        )
        assertFalse(afterDecline.hasOpenOffer("winner"))
        assertTrue(afterDecline.hasOpenOffer("other"))
    }

    @Test
    fun booking_closesTheOffer() {
        val afterBooking = lottery(claimedIds = listOf("winner"))
        assertFalse(afterBooking.hasOpenOffer("winner"))
    }

    @Test
    fun declined_winnerStillCountsAsAWinner() {
        // declining only closes the offer; the draw result itself is not changed
        val afterDecline = lottery(declinedIds = listOf("winner"))
        assertTrue(afterDecline.hasWon("winner"))
        assertEquals(LotteryOutcome.WON, afterDecline.outcomeFor("winner"))
    }
}