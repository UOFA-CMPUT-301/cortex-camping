/**
 * Repository for reservations
 *
 * Reservations live in their own "reservations" collection (not inside the Entrant document)
 * so that every user can see which nights are taken at a campsite. Dates are saved as ISO strings,
 * the same way LotteryRepository does it.
 *
 * "bookedNights" holds one document per booked night, with the id "<campsiteId>_<date>".
 * LotteryRepository.claimWin checks and writes these inside a transaction, so two winners
 * can't be given the same night.
 */

package com.example.cortex_camping.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.model.hasStayDatesLeft
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import com.example.cortex_camping.model.CampsiteRating
class ReservationRepository {
    private val db = Firebase.firestore
    private val reservationsRef = db.collection("reservations")
    private val nightsRef = db.collection("bookedNights")

    private val _reservations = mutableStateListOf<Reservation>()
    private val _nights = mutableStateListOf<Pair<String, LocalDate>>() // campsiteId to night

    val reservations: List<Reservation>
        get() = _reservations

    /**
     * Returns all booked dates for a specific campsite.
     *
     * @param campsiteId the ID of the campsite
     * @return a set of dates that are currently booked at the campsite
     */
    // every night that is booked at this campsite
    fun bookedDates(campsiteId: String): Set<LocalDate> {
        val locked = _nights.filter { it.first == campsiteId }.map { it.second }
        // also count active reservations directly, in case one was saved without its night locks
        val reserved = _reservations
            .filter { it.campsiteId == campsiteId && !it.isWithdrawn }
            .flatMap { r -> (0 until r.nights).map { r.checkIn.plusDays(it.toLong()) } }
        return (locked + reserved).toSet()
    }

    init {
        reservationsRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            _reservations.clear()
            snapshot?.documents?.forEach { document ->
                document.data?.let { fromMap(it) }?.let { _reservations.add(it) }
            }
        }
        nightsRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            _nights.clear()
            snapshot?.documents?.forEach { document ->
                val campsiteId = document.getString("campsiteId")
                val date = document.getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (campsiteId != null && date != null) _nights.add(campsiteId to date)
            }
        }
    }

    /**
     * Cancels a reservation and frees all nights associated with it
     * Updates the reservation status to CANCELLED and deletes its booked
     * nights from Firestore.
     *
     * @param reservation the reservation to cancel
     * @param onResult called with null if successful, or an error message if the operation fails
     */
    fun cancel(reservation: Reservation, userId: String, onResult: (String?) -> Unit = {}) {
        val reservationDoc = reservationsRef.document(reservation.bookingId)

        db.runTransaction { transaction ->
            val data = transaction.get(reservationDoc).data
                ?: throw IllegalStateException("Reservation not found.")
            // use the saved copy, not the one on screen, which may be out of date
            val saved = fromMap(data)
                ?: throw IllegalStateException("Could not read reservation.")

            check(saved.userId == userId) { "You can only cancel your own reservations." }
            check(saved.status != ReservationStatus.CANCELLED) { "This reservation is already cancelled." }
            check(saved.status != ReservationStatus.REJECTED) { "The campsite owner already rejected this reservation." }
            check(!saved.checkOut.isBefore(LocalDate.now())) { "Past stays can't be cancelled." }

            val mine = transaction.ownNightLocks(saved)

            transaction.update(reservationDoc, "status", ReservationStatus.CANCELLED.name)
            mine.forEach { transaction.delete(it) }
            null
        }
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it.message ?: "Could not cancel the reservation.") }
    }

    /**
     * The campsite owner rejects a reservation at one of their campsites. Its nights are freed,
     * and if it was a lottery win whose stay window still has nights ahead, the win opens again
     * so the camper can choose new dates.
     *
     * @param reservation the reservation to reject
     * @param ownerId the owner rejecting it, who has to own the campsite
     * @param reason why it was rejected, shown to the camper; can be blank
     * @param onResult called with an error message or null, and whether the lottery win reopened
     */
    fun reject(
        reservation: Reservation,
        ownerId: String,
        reason: String,
        onResult: (error: String?, winReopened: Boolean) -> Unit = { _, _ -> }
    ) {
        val trimmed = reason.trim()
        val reservationDoc = reservationsRef.document(reservation.bookingId)

        db.runTransaction { transaction ->
            val saved = transaction.get(reservationDoc).data?.let { fromMap(it) }
                ?: throw IllegalStateException("Reservation not found.")
            val campsiteOwner = transaction.get(db.collection("campsite").document(saved.campsiteId))
                .getString("ownerId")

            check(campsiteOwner == ownerId) { "You can only reject reservations at your own campsites." }
            check(saved.status != ReservationStatus.CANCELLED) { "The camper already cancelled this reservation." }
            check(saved.status != ReservationStatus.REJECTED) { "This reservation is already rejected." }
            check(!saved.checkOut.isBefore(LocalDate.now())) { "Past stays can't be rejected." }

            val mine = transaction.ownNightLocks(saved)

            // all reads have to happen before the first write
            val lotteryDoc = saved.lotteryId.takeIf { it.isNotBlank() }
                ?.let { db.collection("lotteries").document(it) }
            val lottery = lotteryDoc?.let { transaction.get(it) }
            val claimed = (lottery?.get("claimedIds") as? List<*>)?.contains(saved.userId) == true
            val stayUntil = lottery?.getString("stayUntil")
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val reopen = claimed && stayUntil != null && hasStayDatesLeft(stayUntil)

            transaction.update(
                reservationDoc,
                mapOf("status" to ReservationStatus.REJECTED.name, "rejectionReason" to trimmed)
            )
            mine.forEach { transaction.delete(it) }
            if (reopen && lotteryDoc != null) {
                transaction.update(lotteryDoc, "claimedIds", FieldValue.arrayRemove(saved.userId))
            }
            reopen
        }
            .addOnSuccessListener { onResult(null, it) }
            .addOnFailureListener { onResult(it.message ?: "Could not reject the reservation.", false) }
    }

    // the night locks that still belong to this booking: someone else may have booked
    // some of those nights since an earlier cancel
    private fun Transaction.ownNightLocks(saved: Reservation): List<DocumentReference> =
        (0 until saved.nights)
            .map { i -> nightsRef.document(nightId(saved.campsiteId, saved.checkIn.plusDays(i.toLong()))) }
            .filter { get(it).getString("bookingId") == saved.bookingId }

    /**
     * Saves the entrant's star rating on their reservation and updates the campsite's
     * average rating
     *
     * @param reservation the stay being rated
     * @param stars the rating, from 1 to 5
     * @param onResult called with null if it was saved, or an error message
     */
    fun rate(reservation: Reservation, stars: Int, onResult: (String?) -> Unit = {}) {
        val reservationDoc = reservationsRef.document(reservation.bookingId)
        val campsiteDoc = db.collection("campsite").document(reservation.campsiteId)
        db.runTransaction { transaction ->
            val stay = transaction.get(reservationDoc)
            val campsite = transaction.get(campsiteDoc)
            check(stay.exists()) { "That reservation no longer exists." }
            check(stay.get("status") == ReservationStatus.CONFIRMED.name) {
                "Only confirmed stays can be rated."
            }
            check(campsite.exists()) { "That campsite no longer exists." }

            val previous = (stay.get("rating") as? Number)?.toInt() ?: 0
            val summary = CampsiteRating.update(
                average = (campsite.get("rating") as? Number)?.toDouble() ?: 0.0,
                count = (campsite.get("ratingCount") as? Number)?.toInt() ?: 0,
                stars = stars,
                previousStars = previous
            )
            transaction.update(reservationDoc, "rating", stars)
            transaction.update(campsiteDoc, mapOf("rating" to summary.average, "ratingCount" to summary.count))
            null
        }
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it.message ?: "Could not save your rating.") }
    }

    companion object {
        /**
         * Generates a unique document ID for a booked campsite night.
         *
         * @param campsiteId the ID of the campsite
         * @param night the booked night
         * @return a document ID combining the campsite ID and date
         */
        fun nightId(campsiteId: String, night: LocalDate) = "${campsiteId}_$night"

        /**
         * Converts a reservation into a map for storing in Firestore.
         * Dates are saved as ISO-formatted strings.
         *
         * @param reservation the reservation to convert
         * @return a map containing the reservation fields
         */
        fun toMap(reservation: Reservation): Map<String, Any> = mapOf(
            "bookingId" to reservation.bookingId,
            "campsiteId" to reservation.campsiteId,
            "userId" to reservation.userId,
            "lotteryId" to reservation.lotteryId,
            "checkIn" to reservation.checkIn.toString(),
            "checkOut" to reservation.checkOut.toString(),
            "guests" to reservation.guests,
            "status" to reservation.status.name,
            "rating" to reservation.rating,
            "rejectionReason" to reservation.rejectionReason
        )

        /**
         * Creates a reservation from a Firestore data map.
         *
         * @param data the reservation fields retrieved from Firestore
         * @return the reconstructed reservation, or null if the data is invalid
         */
        fun fromMap(data: Map<String, Any?>): Reservation? {
            return try {
                Reservation(
                    bookingId = data["bookingId"] as? String ?: "",
                    campsiteId = data["campsiteId"] as? String ?: "",
                    userId = data["userId"] as? String ?: "",
                    lotteryId = data["lotteryId"] as? String ?: "",
                    checkIn = LocalDate.parse(data["checkIn"] as String),
                    checkOut = LocalDate.parse(data["checkOut"] as String),
                    guests = (data["guests"] as? Number)?.toInt() ?: 1,
                    status = (data["status"] as? String)
                        ?.let { ReservationStatus.valueOf(it) }
                        ?: ReservationStatus.CONFIRMED,
                    rating = (data["rating"] as? Number)?.toInt() ?: 0,
                    rejectionReason = data["rejectionReason"] as? String ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}