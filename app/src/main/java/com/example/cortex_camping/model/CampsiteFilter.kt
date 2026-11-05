/*
 * CampsiteFilter.kt
 *
 * Holds the search and filter rules entrants use on Browse screen
 *
 */

package com.example.cortex_camping.model
import java.time.LocalDate
/**
 * Search text and filters the user has chosen on the Browse screen
 *
 * @property query text is matched against the name, location, or tag of a campsite
 * @property minGuests is the smallest group size the campsite has
 * @property location shows the campsites at a location
 * @property checkIn is the first date of stay
 * @property checkOut is the day the user leaves
 */
data class CampsiteFilter (
    val query: String = "",
    val minGuests: Int = 0,
    val location: String = "",
    val checkIn: LocalDate? = null,
    val checkOut: LocalDate? = null,
) {

    /**
     * Checks if the campsite matches the search and all the filters
     *
     * @param campsite the campsite that gets checked
     * @return true if there is a match
     */
    fun matches(campsite: Campsite): Boolean {
        // removes any white space from the text at start or end
        val q = query.trim()
        // checks if what the user typed is in the campsite's name, location or tags
        val queryMatch = q.isBlank() || campsite.name.contains(q, true) ||
                campsite.location.contains(q, true) || campsite.tag.any { it.contains(q, true) }
        // if a location is picked, checks that the campsite is at that location
        val locationMatch = location.isBlank() || campsite.location == location
        return queryMatch && locationMatch && campsite.maxOccupancy >= minGuests && isCampsiteFree(campsite)
    }

    /**
     * Gives back only the campsites that match, in the same order
     *
     * @param campsites list of all the campsites
     * @return the campsites that match
     */
    fun filterCampsites(campsites: List<Campsite>): List<Campsite> = campsites.filter(::matches)

    /**
     * Checks if the campsite is available for each night of the user stay
     * Checks through checkIn to checkOut date
     * checkOut is not included since user leaves that day
     *
     * @param campsite the campsite that gets checked
     * @return true if no dates were picked or if every night is available, false otherwise
     */

    private fun isCampsiteFree(campsite: Campsite): Boolean {
        if (checkIn == null || checkOut == null) return true
        var date: LocalDate = checkIn
        while (date < checkOut) {
            if (!campsite.availability.isAvailable(date)) return false
            date = date.plusDays(1)
        }
        return true
    }
}
