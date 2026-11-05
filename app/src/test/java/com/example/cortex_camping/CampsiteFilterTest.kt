/*
 * CampsiteFilterTest.kt
 *
 * Unit tests for CampsiteFilter
 * Uses real Alberta campgrounds with made up availability for May 2027
 */

package com.example.cortex_camping

import com.example.cortex_camping.model.AvailabilitySchedule
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.CampsiteFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CampsiteFilterTest {

    private fun may(day: Int) = LocalDate.of(2027, 5, day)

    // open all of May but already booked May 15 and 16
    private val twoJack = Campsite(
        campsiteId = "1",
        name = "Two Jack Lakeside",
        location = "Banff National Park, AB",
        maxOccupancy = 6,
        tag = listOf("lakeside"),
        price = 45.0,
        availability = AvailabilitySchedule(may(1), may(31), setOf(may(15), may(16)))
    )

    // only open in the summer
    private val whistlers = Campsite(
        campsiteId = "2",
        name = "Whistlers Campground",
        location = "Jasper National Park, AB",
        maxOccupancy = 8,
        tag = listOf("hiking"),
        price = 55.0,
        availability = AvailabilitySchedule(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 8, 31))
    )

    private val hinton = Campsite(
        campsiteId = "3",
        name = "Hinton Ridge",
        location = "Hinton, AB",
        maxOccupancy = 2,
        tag = listOf("tennis court"),
        price = 65.0,
        availability = AvailabilitySchedule(may(1), may(31))
    )

    private val all = listOf(twoJack, whistlers, hinton)

    @Test
    fun noFiltersShowsEverything() {
        assertEquals(all, CampsiteFilter().filterCampsites(all))
    }

    @Test
    fun searchByName() {
        assertEquals(listOf(whistlers), CampsiteFilter(query = "WHISTLERS").filterCampsites(all))
    }

    @Test
    fun searchByLocation() {
        assertEquals(listOf(twoJack), CampsiteFilter(query = "banff").filterCampsites(all))
    }

    @Test
    fun searchByTag() {
        assertEquals(listOf(hinton), CampsiteFilter(query = "tennis").filterCampsites(all))
    }

    @Test
    fun searchWithNoMatches() {
        assertTrue(CampsiteFilter(query = "toronto").filterCampsites(all).isEmpty())
    }

    @Test
    fun filterByGuests() {
        assertEquals(listOf(twoJack, whistlers), CampsiteFilter(minGuests = 4).filterCampsites(all))
    }

    @Test
    fun filterByLocation() {
        assertEquals(listOf(hinton), CampsiteFilter(location = "Hinton, AB").filterCampsites(all))
    }

    @Test
    fun filterByDatesWhenOpen() {
        val filter = CampsiteFilter(checkIn = may(3), checkOut = may(5))
        assertEquals(listOf(twoJack, hinton), filter.filterCampsites(all))
    }

    @Test
    fun filterByDatesHidesBookedNights() {
        // Two Jack is booked on the 15th
        val filter = CampsiteFilter(checkIn = may(14), checkOut = may(17))
        assertEquals(listOf(hinton), filter.filterCampsites(all))
    }

    @Test
    fun checkOutDayCanBeBooked() {
        // nights of the 13th and 14th, leaving on the 15th
        assertTrue(CampsiteFilter(checkIn = may(13), checkOut = may(15)).matches(twoJack))
    }

    @Test
    fun allFiltersTogether() {
        val filter = CampsiteFilter(query = "AB", minGuests = 4, checkIn = may(3), checkOut = may(5))
        assertEquals(listOf(twoJack), filter.filterCampsites(all))
    }
}