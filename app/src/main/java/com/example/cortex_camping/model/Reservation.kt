package com.example.cortex_camping.model

import java.time.LocalDate

data class Reservation(
    val bookingId: String = "",
    val campsiteId: String = "",
    val userId: String = "",
    val checkIn: LocalDate,
    val checkOut: LocalDate
) {
    fun overlapsWith(other: Reservation): Boolean {
        // this function returns true if two reservations overlap, otherwise false
        return checkIn.isBefore(other.checkOut) && checkOut.isAfter(other.checkIn)
    }
}
