package com.example.cortex_camping

import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.repository.ReservationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class ReservationReviewTest {

    private val stay = Reservation(
        bookingId = "b-1",
        campsiteId = "two-jack",
        userId = "device-1",
        checkIn = LocalDate.of(2027, 5, 26),
        checkOut = LocalDate.of(2027, 5, 28),
    )

    @Test
    fun newReservationHasNoRatingOrReview() {
        assertEquals(0, stay.rating)
        assertEquals("", stay.review)
    }

    @Test
    fun reviewSurvivesSavingAndLoading() {
        val reviewed = stay.copy(rating = 4, review = "Quiet, clean and close to the lake.")
        val loaded = ReservationRepository.fromMap(ReservationRepository.toMap(reviewed))
        assertNotNull(loaded)
        assertEquals(4, loaded!!.rating)
        assertEquals("Quiet, clean and close to the lake.", loaded.review)
    }

    @Test
    fun reservationsSavedBeforeReviewsExistLoadWithABlankReview() {
        // old documents have no "review" field at all, and Firestore returns numbers as Long
        val old = mapOf<String, Any?>(
            "bookingId" to "b-1",
            "campsiteId" to "two-jack",
            "userId" to "device-1",
            "checkIn" to "2027-05-26",
            "checkOut" to "2027-05-28",
            "guests" to 2L,
            "status" to "CONFIRMED",
            "rating" to 5L,
        )
        val loaded = ReservationRepository.fromMap(old)
        assertNotNull(loaded)
        assertEquals(5, loaded!!.rating)
        assertEquals("", loaded.review)
    }

    @Test
    fun reviewDoesNotChangeWhetherAStayIsPast() {
        val today = LocalDate.of(2027, 6, 1)
        assertEquals(true, stay.copy(review = "Great").isPast(today))
        val withdrawn = stay.copy(status = ReservationStatus.CANCELLED, review = "Great")
        assertEquals(true, withdrawn.isWithdrawn)
    }
}