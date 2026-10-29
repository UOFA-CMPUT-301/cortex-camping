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

    fun addEntrant(entrant: Entrant) {
        // server-side addition of entrant to Firestore
        val document = entrantsRef.document()
        val entrantWithId = entrant.copy(id = document.id)
        document.set(entrantWithId)
    }

    fun editEntrant(entrant: Entrant) {
        // to allow users to edit their own information (minus id)
        if (entrant.id.isNotEmpty()) {
            entrantsRef.document(entrant.id).set(entrant)
        }
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
                val entrant = document.toObject(Entrant::class.java)
                if (entrant != null) {
                    val updatedReservations = entrant.reservations.filter { it.bookingId != bookingId }
                    docRef.update("reservations", updatedReservations)
                }
            }
        }
    }

    init {
        entrantsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            _entrants.clear()
            snapshot?.documents?.forEach { document ->
                val entrant = document.toObject(Entrant::class.java)
                if (entrant != null) {
                    _entrants.add(entrant)
                }
            }
        }
    }
}