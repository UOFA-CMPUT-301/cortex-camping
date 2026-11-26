package com.example.cortex_camping.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.Entrant
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/*
The EntrantRepository stores Entrant objects, which have their id, name, email, phone#, reservations, and lotteries.
 */

class EntrantRepository {
    private val db = Firebase.firestore
    private val entrantsRef = db.collection("entrants")
    private val _entrants = mutableStateListOf<Entrant>()

    val entrants: List<Entrant>
        get() = _entrants

    fun saveOrUpdateEntrant(deviceId: String, entrant: Entrant) {
        // now generates a Firestore document with id based on Android device ID
        val entrantWithId = entrant.copy(id = deviceId)
        entrantsRef.document(deviceId).set(entrantToMap(entrantWithId))
    }

    fun deleteEntrant(entrantId: String) {
        // to allow entrants to delete their own profiles
        if (entrantId.isNotBlank()) {
            entrantsRef.document(entrantId).delete()
        }
    }

    fun cancelReservation(entrantId: String, bookingId: String) {
        if (entrantId.isNotBlank()) {
            val docRef = entrantsRef.document(entrantId)
            docRef.get().addOnSuccessListener { document ->
                val entrant = document.data?.let { documentToEntrant(document.id, it) }
                if (entrant != null) {
                    val updatedReservations = entrant.reservations.filter { it.bookingId != bookingId }
                    docRef.update("reservations", updatedReservations.map(ReservationRepository::toMap))
                }
            }
        }
    }

    private fun entrantToMap(entrant: Entrant): Map<String, Any?> {
        return mapOf(
            "id" to entrant.id,
            "name" to entrant.name,
            "email" to entrant.email,
            "phoneNumber" to entrant.phoneNumber,
            "reservations" to entrant.reservations.map(ReservationRepository::toMap),
            "notificationsEnabled" to entrant.notificationsEnabled,
            "lotteryNotifications" to entrant.lotteryNotifications,
            "reservationNotifications" to entrant.reservationNotifications
        )
    }

    private fun documentToEntrant(id: String, data: Map<String, Any?>): Entrant {
        val reservations = (data["reservations"] as? List<*>).orEmpty().mapNotNull { value ->
            val fields = value as? Map<*, *> ?: return@mapNotNull null
            val reservationData = fields.entries.mapNotNull { (key, fieldValue) ->
                (key as? String)?.let { it to fieldValue }
            }.toMap()
            ReservationRepository.fromMap(reservationData)
        }
        return Entrant(
            // Prefer the stored identity used by lottery entries, as in documentToMessage.
            id = (data["id"] as? String)?.ifBlank { id } ?: id,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phoneNumber = data["phoneNumber"] as? String ?: "",
            reservations = reservations,
            notificationsEnabled = data["notificationsEnabled"] as? Boolean ?: true,
            lotteryNotifications = data["lotteryNotifications"] as? Boolean ?: true,
            reservationNotifications = data["reservationNotifications"] as? Boolean ?: false
        )
    }

    init {
        entrantsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            _entrants.clear()
            snapshot?.documents?.forEach { document ->
                try {
                    val entrant = document.toObject(Entrant::class.java)
                    if (entrant != null) {
                        _entrants.add(entrant)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EntrantRepository", "Error parsing entrant $document.id}", e)
                }
            }
        }
    }
}