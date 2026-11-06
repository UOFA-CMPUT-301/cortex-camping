package com.example.cortex_camping.model

import com.google.firebase.firestore.Exclude
import java.time.LocalDate
data class Entrant(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val reservations: List<Reservation> = emptyList(),
    // notification settings from the Profile & Settings screen
    val notificationsEnabled: Boolean = true,
    val lotteryNotifications: Boolean = true,
    val reservationNotifications: Boolean = false
) {
    @get:Exclude
    val initials: String
        // first letter of the first two names, e.g. "Casey Clarke" -> "CC"
        get() = name.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }

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
