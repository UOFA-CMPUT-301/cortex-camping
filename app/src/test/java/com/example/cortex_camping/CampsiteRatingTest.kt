/*
 * CampsiteRatingTest.kt
 *
 * Unit tests for CampsiteRating: who can rate a stay and how the campsite's
 * average rating changes (US 01.13.01).
 */

package com.example.cortex_camping

import com.example.cortex_camping.model.CampsiteRating
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CampsiteRatingTest {

    private val today = LocalDate.of(2027, 6, 1)

    // a 2 night stay that ended a few days ago
    private val pastStay = Reservation(
        bookingId = "b-1",
        campsiteId = "two-jack",
        userId = "device-1",
        checkIn = LocalDate.of(2027, 5, 26),
        checkOut = LocalDate.of(2027, 5, 28)
    )

    // Who can rate

    @Test
    fun pastStayCanBeRated() {
        assertNull(CampsiteRating.problem(pastStay, 4, today))
    }

    @Test
    fun stayEndingTodayCanBeRated() {
        assertNull(CampsiteRating.problem(pastStay.copy(checkOut = today), 4, today))
    }

    @Test
    fun upcomingStayCantBeRated() {
        val upcoming = pastStay.copy(checkIn = today.plusDays(3), checkOut = today.plusDays(5))
        assertEquals("You can rate this stay after you check out.", CampsiteRating.problem(upcoming, 4, today))
    }

    @Test
    fun cancelledStayCantBeRated() {
        val cancelled = pastStay.copy(status = ReservationStatus.CANCELLED)
        assertEquals("You can't rate a cancelled stay.", CampsiteRating.problem(cancelled, 4, today))
    }

    @Test
    fun starsMustBeOneToFive() {
        assertEquals("Pick between 1 and 5 stars.", CampsiteRating.problem(pastStay, 0, today))
        assertEquals("Pick between 1 and 5 stars.", CampsiteRating.problem(pastStay, 6, today))
    }

    // How the average changes

    @Test
    fun firstRatingBecomesTheAverage() {
        val summary = CampsiteRating.update(average = 0.0, count = 0, stars = 4, previousStars = 0)
        assertEquals(4.0, summary.average, 0.0001)
        assertEquals(1, summary.count)
    }

    @Test
    fun newRatingIsAddedToTheAverage() {
        // 4.0 from 3 ratings is 12 stars, plus 2 is 14 stars from 4 ratings
        val summary = CampsiteRating.update(average = 4.0, count = 3, stars = 2, previousStars = 0)
        assertEquals(3.5, summary.average, 0.0001)
        assertEquals(4, summary.count)
    }

    @Test
    fun changingARatingDoesNotCountTwice() {
        // 4.0 from 3 ratings, and this stay's 3 stars become 5: 12 - 3 + 5 = 14 from 3 ratings
        val summary = CampsiteRating.update(average = 4.0, count = 3, stars = 5, previousStars = 3)
        assertEquals(14.0 / 3, summary.average, 0.0001)
        assertEquals(3, summary.count)
    }

    @Test
    fun missingCountIsTreatedAsAFirstRating() {
        // the stay says it was rated before but the campsite has no ratings, so start fresh
        val summary = CampsiteRating.update(average = 0.0, count = 0, stars = 5, previousStars = 3)
        assertEquals(5.0, summary.average, 0.0001)
        assertEquals(1, summary.count)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateRejectsStarsOutOfRange() {
        CampsiteRating.update(average = 4.0, count = 3, stars = 7, previousStars = 0)
    }
}