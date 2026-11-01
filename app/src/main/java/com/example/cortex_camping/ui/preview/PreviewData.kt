package com.example.cortex_camping.ui.preview

import com.example.cortex_camping.model.AvailabilitySchedule
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.model.Reservation
import java.time.LocalDate
import java.time.LocalDateTime

object PreviewData {
    const val USER_ID = "preview-user"

    private val today = LocalDate.now()

    val campsite = Campsite(
        ownerId = "preview-owner",
        campsiteId = "raven-peak",
        name = "Raven Peak",
        description = "A peaceful pine forest retreat with mountain views, lakeside trails and room to unwind.",
        tag = listOf("Tent site", "Water", "Fire ring", "Picnic table", "Toilets"),
        maxOccupancy = 6,
        price = 45.0,
        availability = AvailabilitySchedule(today, today.plusMonths(6)),
        rating = 4.8,
        ratingCount = 124,
        location = "Big Bear Lake, CA",
    )

    val reservation = Reservation(
        bookingId = "r1",
        campsiteId = campsite.campsiteId,
        userId = USER_ID,
        checkIn = today.plusDays(10),
        checkOut = today.plusDays(12),
        guests = 4,
    )

    val lottery = Lottery(
        id = "l1",
        ownerId = campsite.ownerId,
        name = "December lottery",
        campsiteIds = listOf(campsite.campsiteId),
        capacity = 50,
        winners = 8,
        waitlistCapacity = 20,
        entries = List(24) { LotteryEntry("entrant-$it", campsite.campsiteId) } + LotteryEntry(USER_ID, campsite.campsiteId),
        opensAt = LocalDateTime.now().minusDays(3),
        closesAt = LocalDateTime.now().plusDays(4),
        drawAt = LocalDateTime.now().plusDays(5),
        stayFrom = today.plusMonths(1),
        stayUntil = today.plusMonths(2),
    )
}
