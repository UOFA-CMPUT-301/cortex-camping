/*
 * ReservationNotificationsTest.kt
 *
 * Unit tests for the reservation confirmation (US 01.05.01, US 01.05.02)
 * and cancellation confirmation (US 01.07.02) notifications.
 */

package com.example.cortex_camping

import com.example.cortex_camping.model.NotificationKind
import com.example.cortex_camping.model.NotificationTarget
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationNotifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReservationNotificationsTest {

    // 2 nights at Two Jack for a group of 4
    private val stay = Reservation(
        bookingId = "b-123",
        campsiteId = "two-jack",
        userId = "device-1",
        checkIn = LocalDate.of(2027, 5, 3),
        checkOut = LocalDate.of(2027, 5, 5),
        guests = 4
    )

    @Test
    fun confirmationGoesToTheEntrantWhoBooked() {
        val n = ReservationNotifications.confirmed(stay, "Two Jack Lakeside")
        assertEquals("device-1", n.recipientId)
        assertEquals("two-jack", n.campsiteId)
        assertFalse(n.read)
        assertFalse(n.forOwner)
    }

    @Test
    fun confirmationNamesTheCampsite() {
        val n = ReservationNotifications.confirmed(stay, "Two Jack Lakeside")
        assertEquals("Reservation confirmed: Two Jack Lakeside", n.title)
        assertEquals(NotificationKind.SUCCESS, n.kind)
        assertEquals(NotificationTarget.MY_RESERVATIONS, n.target)
    }

    @Test
    fun detailsHaveDatesNightsGuestsAndBookingId() {
        assertEquals(
            "May 3, 2027 – May 5, 2027 · 2 nights · 4 guests · Booking b-123",
            ReservationNotifications.details(stay)
        )
    }

    @Test
    fun detailsUseSingularForOneNightAndOneGuest() {
        val short = stay.copy(checkOut = LocalDate.of(2027, 5, 4), guests = 1)
        assertTrue(ReservationNotifications.details(short).contains("1 night · 1 guest"))
    }

    @Test
    fun cancellationSaysTheNightsWereReleased() {
        val n = ReservationNotifications.cancelled(stay, "Two Jack Lakeside")
        assertEquals("Reservation cancelled: Two Jack Lakeside", n.title)
        assertEquals(NotificationKind.CANCELLED, n.kind)
        assertTrue(n.body.startsWith("May 3, 2027 – May 5, 2027"))
        assertTrue(n.body.endsWith("The nights have been released."))
        assertEquals("device-1", n.recipientId)
    }
}