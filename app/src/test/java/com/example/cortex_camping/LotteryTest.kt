/*
 * LotteryTest.kt
 *
 * Unit tests for the owner's lottery settings: validation, registration period,
 * waitlist capacity and applying edits to a running lottery
 */

package com.example.cortex_camping

import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.model.NO_WAITLIST_LIMIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class LotteryTest {

    private fun may(day: Int) = LocalDateTime.of(2027, 5, day, 12, 0)
    private fun june(day: Int) = LocalDate.of(2027, 6, day)
    private fun entries(vararg ids: String) = ids.map { LotteryEntry(it, "site") }

    // registration runs May 1 to 10, the draw is May 12, stays are June 1 to 30
    private val lottery = Lottery(
        id = "lotto",
        ownerId = "owner",
        name = "Summer draw",
        campsiteIds = listOf("site"),
        capacity = 2,
        winners = 1,
        opensAt = may(1),
        closesAt = may(10),
        drawAt = may(12),
        stayFrom = june(1),
        stayUntil = june(30),
        nights = 2
    )

    @Test
    fun validSettingsHaveNoError() {
        assertNull(lottery.settingsError)
    }

    @Test
    fun capacityAndWinnersMustBePositive() {
        assertEquals("Lottery capacity must be at least 1.", lottery.copy(capacity = 0).settingsError)
        assertEquals("A lottery needs at least 1 winner.", lottery.copy(winners = 0).settingsError)
    }

    @Test
    fun winnersCantBeMoreThanCapacity() {
        assertEquals("There can't be more winners than lottery entries.", lottery.copy(winners = 3).settingsError)
    }

    @Test
    fun registrationMustCloseAfterItOpens() {
        assertEquals("Entry has to close after it opens.", lottery.copy(closesAt = may(1)).settingsError)
    }

    @Test
    fun drawCantBeBeforeRegistrationCloses() {
        assertEquals("The draw can't be before entry closes.", lottery.copy(drawAt = may(9)).settingsError)
    }

    @Test
    fun bookingPeriodMustEndAfterItStarts() {
        assertEquals("Stays need to end after they start.", lottery.copy(stayUntil = june(1)).settingsError)
    }

    @Test
    fun bookingPeriodCantStartBeforeTheDraw() {
        assertEquals("Stays can't start before the draw.", lottery.copy(stayFrom = LocalDate.of(2027, 5, 11)).settingsError)
    }

    @Test
    fun bookingPeriodMustFitTheStay() {
        assertEquals("The stay period is too short for 2 nights.", lottery.copy(stayUntil = june(2)).settingsError)
        assertNull(lottery.copy(stayUntil = june(3)).settingsError)
    }

    @Test
    fun registrationIsOnlyOpenDuringThePeriod() {
        assertFalse(lottery.isRegistrationOpen(may(1).minusMinutes(1)))
        assertTrue(lottery.isRegistrationOpen(may(1)))
        assertTrue(lottery.isRegistrationOpen(may(10)))
        assertFalse(lottery.isRegistrationOpen(may(10).plusMinutes(1)))
    }

    @Test
    fun registrationIsClosedOnceFinalized() {
        assertFalse(lottery.copy(status = LotteryStatus.FINALIZED).isRegistrationOpen(may(5)))
    }

    @Test
    fun waitlistIsOffAtZeroCapacity() {
        assertFalse(lottery.hasWaitlist)
    }

    @Test
    fun waitlistWithACapacityShowsTheLimit() {
        val capped = lottery.copy(waitlistCapacity = 5, waitlist = entries("a"))
        assertTrue(capped.hasWaitlist)
        assertEquals(5, capped.waitlistLimit)
        assertEquals("1/5", capped.waitlistSummary)
    }

    @Test
    fun waitlistCapacityIsOptional() {
        val uncapped = lottery.copy(waitlistCapacity = NO_WAITLIST_LIMIT, waitlist = entries("a"))
        assertTrue(uncapped.hasWaitlist)
        assertFalse(uncapped.hasWaitlistLimit)
        assertEquals(Int.MAX_VALUE, uncapped.waitlistLimit)
        assertEquals("1", uncapped.waitlistSummary)
    }

    @Test
    fun editsKeepPeopleWhoJoinedWhileTheFormWasOpen() {
        val live = lottery.copy(entries = entries("a", "b"), waitlist = entries("c"))
        val updated = live.withSettingsFrom(lottery.copy(name = "Renamed", winners = 2))

        assertEquals("Renamed", updated.name)
        assertEquals(2, updated.winners)
        assertEquals(live.entries, updated.entries)
        assertEquals(live.waitlist, updated.waitlist)
    }

    @Test
    fun editsCantChangeTheOwnerOrStatus() {
        val updated = lottery.withSettingsFrom(lottery.copy(ownerId = "someone else", status = LotteryStatus.FINALIZED))

        assertEquals("owner", updated.ownerId)
        assertEquals(LotteryStatus.ONGOING, updated.status)
    }

    @Test
    fun raisingCapacityMovesTheWaitlistIntoTheNewSlots() {
        val live = lottery.copy(entries = entries("a", "b"), waitlist = entries("c", "d"))
        val updated = live.withSettingsFrom(live.copy(capacity = 3))

        assertEquals(entries("a", "b", "c"), updated.entries)
        assertEquals(entries("d"), updated.waitlist)
    }
}
