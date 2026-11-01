package com.example.cortex_camping.model

import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ReservationStatus(val label: String) {
    CONFIRMED("Confirmed"),
    PENDING("Pending"),
    CANCELLED("Cancelled")
}

data class Reservation(
    val bookingId: String = "",
    val campsiteId: String = "",
    val userId: String = "",
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guests: Int = 1,
    val status: ReservationStatus = ReservationStatus.CONFIRMED,
    val rating: Int = 0 // 0 means the entrant hasn't rated the stay yet
) {
    @get:Exclude
    val nights: Int
        get() = ChronoUnit.DAYS.between(checkIn, checkOut).toInt()

    fun overlapsWith(other: Reservation): Boolean {
        // this function returns true if two reservations overlap, otherwise false
        return checkIn.isBefore(other.checkOut) && checkOut.isAfter(other.checkIn)
    }
}
