package com.example.cortex_camping.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.CampsiteOwner
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class CampsiteOwnerRepository {
    private val db = Firebase.firestore
    private val ownersRef = db.collection("owners")
    private val _owners = mutableStateListOf<CampsiteOwner>()

    val owners: List<CampsiteOwner>
        get() = _owners

    fun addOwner(owner: CampsiteOwner) {
        ownersRef.document(owner.id).set(owner)
    }

    fun editOwnerName(owner: CampsiteOwner) {
        // we don't allow editing IDs lol
        ownersRef.document(owner.name).set(owner)
    }

    init {
        ownersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            _owners.clear()

            snapshot?.documents?.forEach { document ->
                val owner = document.toObject(CampsiteOwner::class.java)
                if (owner != null) {
                    _owners.add(owner)
                }
            }
        }
    }
}