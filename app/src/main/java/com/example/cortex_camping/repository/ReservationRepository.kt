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
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDate

class ReservationRepository {
    private val db = Firebase.firestore
    private val reservationsRef = db.collection("reservations")
    private val nightsRef = db.collection("bookedNights")

    private val _reservations = mutableStateListOf<Reservation>()
    private val _nights = mutableStateListOf<Pair<String, LocalDate>>() // campsiteId to night

    val reservations: List<Reservation>
        get() = _reservations

    // every night that is booked at this campsite
    fun bookedDates(campsiteId: String): Set<LocalDate> =
        _nights.filter { it.first == campsiteId }.map { it.second }.toSet()

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

    // marks the reservation cancelled and frees its nights for other people
    fun cancel(reservation: Reservation, onResult: (String?) -> Unit = {}) {
        val batch = db.batch()
        batch.update(
            reservationsRef.document(reservation.bookingId),
            "status", ReservationStatus.CANCELLED.name
        )
        for (i in 0 until reservation.nights) {
            val night = reservation.checkIn.plusDays(i.toLong())
            batch.delete(nightsRef.document(nightId(reservation.campsiteId, night)))
        }
        batch.commit()
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it.message ?: "Could not cancel the reservation.") }
    }

    companion object {
        fun nightId(campsiteId: String, night: LocalDate) = "${campsiteId}_$night"

        fun toMap(reservation: Reservation): Map<String, Any> = mapOf(
            "bookingId" to reservation.bookingId,
            "campsiteId" to reservation.campsiteId,
            "userId" to reservation.userId,
            "lotteryId" to reservation.lotteryId,
            "checkIn" to reservation.checkIn.toString(),
            "checkOut" to reservation.checkOut.toString(),
            "guests" to reservation.guests,
            "status" to reservation.status.name,
            "rating" to reservation.rating
        )

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
                    rating = (data["rating"] as? Number)?.toInt() ?: 0
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}