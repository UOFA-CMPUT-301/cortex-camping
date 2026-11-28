/*
 * CampsiteRating.kt
 *
 * Rules for rating a campsite after a stay  who can rate,
 * and how the campsite's average changes. Rating the same stay again replaces
 * the old rating instead of counting twice.
 *
 */

package com.example.cortex_camping.model

import java.time.LocalDate

/**
 * A campsite's average rating and how many ratings it's based on.
 *
 * @property average the average number of stars
 * @property count how many stays have been rated
 */
data class RatingSummary(val average: Double, val count: Int)

object CampsiteRating {
     const val MAX_REVIEW_LENGTH = 500
    /**
     * Checks if this stay can be rated.
     *
     * @param reservation the stay being rated
     * @param stars how many stars the user picked
     * @param today today's date
     * @return why it can't be rated, or null if it can
     */

    fun problem(reservation: Reservation, stars: Int, review: String, today: LocalDate, userId: String): String? {
        if (reservation.userId != userId) return "You can only review stays you booked."
        if (stars !in 1..5) return "Pick between 1 and 5 stars."
        if (review.trim().length > MAX_REVIEW_LENGTH) return "Reviews can only be up to $MAX_REVIEW_LENGTH characters."
        if (reservation.status == ReservationStatus.CANCELLED) return "You can't rate a cancelled stay."
        if (reservation.status == ReservationStatus.REJECTED) return "You can't rate a rejected stay."
        if (reservation.checkOut.isAfter(today)) return "You can rate this stay after you check out."
        return null
    }

    /**
     * Works out the campsite's new average after a stay is rated.
     *
     * @param average the campsite's current average
     * @param count how many ratings the average is based on
     * @param stars the new rating
     * @param previousStars this stay's old rating, 0 if it wasn't rated before
     * @return the new average and count
     */
    fun update(average: Double, count: Int, stars: Int, previousStars: Int): RatingSummary {
        require(stars in 1..5) { "stars must be between 1 and 5" }
        // first time this stay is rated: add one more rating
        if (previousStars == 0 || count == 0) {
            val newCount = count + 1
            return RatingSummary((average * count + stars) / newCount, newCount)
        }
        // rated before: swap the old stars for the new ones
        return RatingSummary((average * count - previousStars + stars) / count, count)
    }
}