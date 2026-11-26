package com.example.cortex_camping

import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.model.past
import com.example.cortex_camping.model.upcoming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for the active and past reservation lists and for which reservations can be cancelled.
 * "Today" is passed in, so the tests don't depend on the real date.
 *
 * Goes over User Stories USER: US 01.06.01, 01.06.02, and 01.07.01
 */
class ReservationListTest {

    private val today = LocalDate.of(2027, 7, 10)

    private fun day(offset: Int) = today.plusDays(offset.toLong())

    private fun reservation(
        id: String,
        checkIn: Int,
        checkOut: Int,
        status: ReservationStatus = ReservationStatus.CONFIRMED
    ) = Reservation(
        bookingId = id,
        campsiteId = "A",
        userId = "user",
        checkIn = day(checkIn),
        checkOut = day(checkOut),
        status = status
    )

    // View a list of my active reservations

    @Test
    fun upcoming_listsFutureStays_notPastOnes() {
        val future = reservation("future", 5, 7)
        val old = reservation("old", -10, -8)

        assertEquals(listOf(future), listOf(future, old).upcoming(today))
    }

    @Test
    fun upcoming_staysEndingTodayStillCount() {
        val endsToday = reservation("today", -2, 0)
        assertEquals(listOf(endsToday), listOf(endsToday).upcoming(today))
    }

    @Test
    fun upcoming_isSortedByCheckInSoonestFirst() {
        val later = reservation("later", 20, 22)
        val sooner = reservation("sooner", 3, 5)

        assertEquals(listOf(sooner, later), listOf(later, sooner).upcoming(today))
    }

    // View a list of my past reservations

    @Test
    fun past_listsFinishedStays_notFutureOnes() {
        val future = reservation("future", 5, 7)
        val old = reservation("old", -10, -8)

        assertEquals(listOf(old), listOf(future, old).past(today))
    }

    @Test
    fun past_isSortedMostRecentFirst() {
        val older = reservation("older", -30, -28)
        val recent = reservation("recent", -5, -3)

        assertEquals(listOf(recent, older), listOf(older, recent).past(today))
    }

    @Test
    fun everyReservationIsInExactlyOneList() {
        val all = listOf(
            reservation("future", 5, 7),
            reservation("endsToday", -2, 0),
            reservation("old", -10, -8),
            reservation("cancelledFuture", 5, 7, ReservationStatus.CANCELLED),
            reservation("cancelledOld", -10, -8, ReservationStatus.CANCELLED)
        )

        val upcoming = all.upcoming(today)
        val past = all.past(today)

        assertEquals(all.size, upcoming.size + past.size)
        assertTrue(upcoming.none { it in past })
    }

    // US 01.07.01 Cancel my campsite reservation

    @Test
    fun cancelledReservation_leavesTheActiveList_andJoinsHistory() {
        val booked = reservation("booked", 5, 7)
        val cancelled = booked.copy(status = ReservationStatus.CANCELLED)

        assertTrue(listOf(booked).upcoming(today).contains(booked))
        assertTrue(listOf(cancelled).upcoming(today).isEmpty())
        assertEquals(listOf(cancelled), listOf(cancelled).past(today))
    }

    @Test
    fun onlyActiveReservationsCanBeCancelled() {
        assertTrue(reservation("future", 5, 7).isActive(today))
        assertFalse(reservation("old", -10, -8).isActive(today))
        assertFalse(reservation("cancelled", 5, 7, ReservationStatus.CANCELLED).isActive(today))
    }
}