/*
 * ReservationRejectionTest.kt
 *
 * Unit tests for reservations a campsite owner rejects: where they show up,
 * what the camper is told, and when a lottery win reopens.
 *
 * Goes over User Story CO: US 02.08.01
 */

package com.example.cortex_camping

import com.example.cortex_camping.model.CampsiteRating
import com.example.cortex_camping.model.NotificationTarget
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationNotifications
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.model.hasStayDatesLeft
import com.example.cortex_camping.model.past
import com.example.cortex_camping.model.upcoming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReservationRejectionTest {

    private val today = LocalDate.of(2027, 7, 10)

    private val rejected = Reservation(
        bookingId = "b1",
        campsiteId = "A",
        userId = "user",
        checkIn = today.plusDays(5),
        checkOut = today.plusDays(7),
        status = ReservationStatus.REJECTED,
        rejectionReason = "Flooding on the site"
    )

    @Test
    fun `a rejected stay leaves the upcoming list and joins history`() {
        assertTrue(listOf(rejected).upcoming(today).isEmpty())
        assertEquals(listOf(rejected), listOf(rejected).past(today))
        assertFalse(rejected.isActive(today))
    }

    @Test
    fun `rejected and cancelled stays are both withdrawn`() {
        assertTrue(rejected.isWithdrawn)
        assertTrue(rejected.copy(status = ReservationStatus.CANCELLED).isWithdrawn)
        assertFalse(rejected.copy(status = ReservationStatus.CONFIRMED).isWithdrawn)
        assertFalse(rejected.copy(status = ReservationStatus.PENDING).isWithdrawn)
    }

    @Test
    fun `a rejected stay can't be rated`() {
        assertEquals(
            "You can't review a rejected stay.",
            CampsiteRating.problem(
                rejected.copy(checkOut = today.minusDays(1)), 5, "", today, rejected.userId
            )
        )
    }

    @Test
    fun `the camper is told the reason`() {
        val n = ReservationNotifications.rejected(rejected, "Lakeside", winReopened = false)

        assertEquals("user", n.recipientId)
        assertEquals("Reservation rejected: Lakeside", n.title)
        assertTrue(n.body.endsWith("Reason: Flooding on the site"))
        assertEquals("View reservations →", n.link)
        assertEquals(NotificationTarget.MY_RESERVATIONS, n.target)
    }

    @Test
    fun `the reason is optional`() {
        val n = ReservationNotifications.rejected(rejected.copy(rejectionReason = ""), "Lakeside", winReopened = false)
        assertFalse(n.body.contains("Reason"))
    }

    @Test
    fun `a reopened win tells the camper to choose new dates`() {
        val n = ReservationNotifications.rejected(rejected, "Lakeside", winReopened = true)

        assertTrue(n.body.endsWith("Your lottery win is open again, so you can choose new dates."))
        assertEquals("Choose new dates →", n.link)
    }

    @Test
    fun `a win only reopens while a night of the stay window is still ahead`() {
        val stayUntil = LocalDate.of(2027, 8, 31)
        assertTrue(hasStayDatesLeft(stayUntil, LocalDate.of(2027, 8, 1)))
        assertTrue(hasStayDatesLeft(stayUntil, LocalDate.of(2027, 8, 30)))
        assertFalse(hasStayDatesLeft(stayUntil, LocalDate.of(2027, 8, 31)))
        assertFalse(hasStayDatesLeft(stayUntil, LocalDate.of(2027, 9, 5)))
    }
}
