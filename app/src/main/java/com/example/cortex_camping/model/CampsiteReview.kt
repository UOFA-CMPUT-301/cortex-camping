package com.example.cortex_camping.model

import java.time.LocalDate

/** One camper's review of a stay, as shown on a campsite's reviews page. */
data class CampsiteReview(
    val bookingId: String,   // unique per stay, used as the list key in the UI
    val author: String,      // the camper's name
    val stars: Int,
    val text: String,        // blank if they only left stars
    val stayEnd: LocalDate   // the stay's check-out date
)