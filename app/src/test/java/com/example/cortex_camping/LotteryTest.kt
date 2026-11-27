/*
 * LotteryTest.kt
 *
 * Unit tests for the owner's lottery settings: validation, registration period,
 * waitlist capacity and applying edits to a running lottery
 */

package com.example.cortex_camping

import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.model.LotteryOutcome
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

    @Test
    fun `the draw can only be run once entry has closed`() {
        assertFalse(lottery.canRunDraw(may(10)))
        assertTrue(lottery.canRunDraw(may(10).plusMinutes(1)))
        assertTrue(lottery.canRunDraw(may(11)))
    }

    @Test
    fun `a lottery that has been drawn can't be drawn again`() {
        assertFalse(lottery.copy(status = LotteryStatus.FINALIZED).canRunDraw(may(11)))
    }

    @Test
    fun `older lotteries count their creator as an owner`() {
        assertEquals(listOf("owner"), lottery.allOwnerIds)
        assertTrue(lottery.isOwner("owner"))
        assertTrue(lottery.isCreator("owner"))
    }

    @Test
    fun `an invited owner isn't an owner until they accept`() {
        val invited = lottery.copy(invitedOwnerIds = listOf("partner"))
        assertTrue(invited.isInvited("partner"))
        assertFalse(invited.isOwner("partner"))
        assertTrue(lottery.copy(ownerIds = listOf("owner", "partner")).isOwner("partner"))
    }

    @Test
    fun `an owner's campsites change without touching anyone else's`() {
        val shared = lottery.copy(campsiteIds = listOf("site", "p1", "p2"))
        val updated = shared.withOwnerCampsites(owned = setOf("p1", "p2", "p3"), chosen = listOf("p2", "p3"))
        assertEquals(listOf("site", "p2", "p3"), updated.campsiteIds)
    }

    @Test
    fun `an owner can't list someone else's campsite`() {
        val updated = lottery.withOwnerCampsites(owned = setOf("p1"), chosen = listOf("p1", "site-from-someone-else"))
        assertEquals(listOf("site", "p1"), updated.campsiteIds)
    }

    @Test
    fun `older lotteries are entered through their first campsite`() {
        assertEquals("site", lottery.entryCampsiteId)
        assertEquals("p1", lottery.copy(campsiteIds = listOf("site", "p1"), featuredCampsiteId = "p1").entryCampsiteId)
    }

    @Test
    fun `the featured campsite has to be in the lottery`() {
        assertEquals("Choose a featured campsite.", lottery.copy(featuredCampsiteId = "elsewhere").settingsError)
    }

    @Test
    fun `changing the featured campsite moves every entry to it`() {
        val live = lottery.copy(
            campsiteIds = listOf("site", "site2"),
            featuredCampsiteId = "site",
            entries = entries("a", "c"),
            waitlist = entries("b")
        )
        val updated = live.withSettingsFrom(live.copy(featuredCampsiteId = "site2"), editorCampsites = setOf("site", "site2"))

        assertEquals("site2", updated.featuredCampsiteId)
        assertEquals(listOf(LotteryEntry("a", "site2"), LotteryEntry("c", "site2")), updated.entries)
        assertEquals(listOf(LotteryEntry("b", "site2")), updated.waitlist)
    }

    @Test
    fun `a cancelled lottery takes no entries and is never drawn`() {
        val cancelled = lottery.copy(status = LotteryStatus.CANCELLED, entries = entries("a"))
        assertFalse(cancelled.isRegistrationOpen(may(5)))
        assertFalse(cancelled.canRunDraw(may(11)))
        assertEquals(LotteryOutcome.CANCELLED, cancelled.outcomeFor("a"))
    }

    @Test
    fun `the creator's edits keep the other owners and their campsites`() {
        val live = lottery.copy(
            campsiteIds = listOf("site", "p1"),
            ownerIds = listOf("owner", "partner"),
            invitedOwnerIds = listOf("pending")
        )
        val edited = live.copy(campsiteIds = listOf("site2"), ownerIds = emptyList(), invitedOwnerIds = emptyList())
        val updated = live.withSettingsFrom(edited, editorCampsites = setOf("site", "site2"))

        assertEquals(listOf("p1", "site2"), updated.campsiteIds)
        assertEquals(listOf("owner", "partner"), updated.ownerIds)
        assertEquals(listOf("pending"), updated.invitedOwnerIds)
    }
}
