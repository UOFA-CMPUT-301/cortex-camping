package com.example.cortex_camping.model

import java.time.LocalDate

data class AvailabilitySchedule(
    val availableFrom: LocalDate, // the first date that the campsite can be booked
    val availableUntil: LocalDate, // the last date that the campsite can be booked
    val unavailableDates: Set<LocalDate> = emptySet()
) {
    init {
        require(!availableUntil.isBefore(availableFrom)) { // checks whether end date comes before start date
            "The end date must be on or after the start date."
        }
    }

    fun isAvailable(date: LocalDate): Boolean {
        // this function returns TRUE or FALSE, used in CampsiteOwner
        return !date.isBefore(availableFrom) &&
                !date.isAfter(availableUntil) &&
                date !in unavailableDates
    }
}