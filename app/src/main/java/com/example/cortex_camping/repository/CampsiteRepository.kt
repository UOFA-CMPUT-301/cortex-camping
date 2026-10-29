package com.example.cortex_camping.repository

import com.example.cortex_camping.model.Campsite
import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


class CampsiteRepository {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsite")
    private val _campsites = mutableStateListOf<Campsite>()
    // campsite has several attributes, see Campsite.kt

    val campsites: List<Campsite>
        get() = _campsites

    fun addCampsite(campsite: Campsite) {
        campsitesRef
            .document(campsite.campsiteId)
            .set(campsite)
    }

    fun editCampsite(campsite: Campsite) {
        campsitesRef
            .document(campsite.campsiteId) // each campsite has a unique id, the id will not be changed
            .set(campsite) // overwrites (effectively updating) the entire updated Campsite object to Firestore
    }

    fun getCampsitesByOwner(
        ownerId: String,
        onResult: (List<Campsite>) -> Unit
    ) {
        campsitesRef
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener {snapshot ->
                val campsites = snapshot.documents.mapNotNull {
                    it.toObject(Campsite::class.java)
                }
                onResult(campsites)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    init {
        campsitesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            _campsites.clear()
            snapshot?.documents?.forEach {document ->
                val campsite = document.toObject(Campsite::class.java)
                if (campsite != null) {
                    _campsites.add(campsite)
                }
            }
        }
    }
}

