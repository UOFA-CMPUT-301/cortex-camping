package com.example.cortex_camping.model

import java.time.LocalDate
data class Entrant(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val reservations: List<Reservation> = emptyList()
) {
    val activeReservations: List<Reservation>
        // returns reservations where the checkOut date is today or later
        get() {
            val today = LocalDate.now()
            return reservations.filter { !it.checkOut.isBefore(today)} // note the !it
        }

    val pastReservations: List<Reservation>
        // returns reservations where checkOut date is before today's date
        get() {
            val today = LocalDate.now()
            return reservations.filter { it.checkOut.isBefore(today)} // note the it
        }
}
