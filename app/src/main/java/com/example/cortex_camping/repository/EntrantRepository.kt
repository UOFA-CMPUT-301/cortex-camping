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
        val document = entrantsRef.document()
        val entrantWithId = entrant.copy(id = document.id)
        document.set(entrantWithId)
    }

    fun editEntrantName(entrant: Entrant) {
        // again, we won't be allowing editing IDs, but we'll allow editing names
        entrantsRef.document(entrant.id).update("name", entrant.name)
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