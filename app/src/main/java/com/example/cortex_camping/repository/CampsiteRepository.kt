package com.example.cortex_camping.repository

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.AvailabilitySchedule
import com.example.cortex_camping.model.Campsite
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDate

class CampsiteRepository {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsite")
    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite>
        get() = _campsites

    fun addCampsite(campsite: Campsite) {
        val id = campsite.campsiteId.ifBlank { campsitesRef.document().id }
        val campsiteWithId = campsite.copy(campsiteId = id)
        val idx = _campsites.indexOfFirst { it.campsiteId == id }
        if (idx >= 0) {
            _campsites[idx] = campsiteWithId
        } else {
            _campsites.add(campsiteWithId)
        }
        campsitesRef.document(id).set(campsiteToMap(campsiteWithId))
            .addOnSuccessListener {
                Log.d("CampsiteRepository", "Successfully written campsite $id to Firestore")
            }
            .addOnFailureListener { error ->
                Log.e("CampsiteRepository", "Error writing campsite $id to Firestore", error)
            }
    }

    fun editCampsite(campsite: Campsite) {
        addCampsite(campsite)
    }

    fun getCampsitesByOwner(
        ownerId: String,
        onResult: (List<Campsite>) -> Unit
    ) {
        campsitesRef
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { documentToCampsite(doc.id, it) }
                }
                onResult(result)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    private fun campsiteToMap(campsite: Campsite): Map<String, Any?> {
        return mapOf(
            "ownerId" to campsite.ownerId,
            "campsiteId" to campsite.campsiteId,
            "name" to campsite.name,
            "description" to campsite.description,
            "tag" to campsite.tag,
            "maxOccupancy" to campsite.maxOccupancy,
            "price" to campsite.price,
            "imageUrl" to campsite.imageUrl,
            "availability" to mapOf(
                "availableFrom" to campsite.availability.availableFrom.toString(),
                "availableUntil" to campsite.availability.availableUntil.toString(),
                "unavailableDates" to campsite.availability.unavailableDates.map { it.toString() }
            ),
            "rating" to campsite.rating,
            "ratingCount" to campsite.ratingCount,
            "location" to campsite.location,
            "rulesUrl" to campsite.rulesUrl
        )
    }

    private fun documentToCampsite(id: String, data: Map<String, Any?>): Campsite {
        val availMap = data["availability"] as? Map<*, *>
        val availFrom = (availMap?.get("availableFrom") as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        val availUntil = (availMap?.get("availableUntil") as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now().plusYears(1)
        val unavail = (availMap?.get("unavailableDates") as? List<*>)?.mapNotNull {
            (it as? String)?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull() }
        }?.toSet() ?: emptySet()

        return Campsite(
            ownerId = data["ownerId"] as? String ?: "",
            // Use the document path so edits and deletions target the record we read.
            campsiteId = id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            tag = (data["tag"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            maxOccupancy = (data["maxOccupancy"] as? Number)?.toInt() ?: 1,
            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
            imageUrl = data["imageUrl"] as? String,
            availability = AvailabilitySchedule(availFrom, availUntil, unavail),
            rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
            ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0,
            location = data["location"] as? String ?: "",
            rulesUrl = data["rulesUrl"] as? String ?: ""
        )
    }

    init {
        campsitesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("CampsiteRepository", "Snapshot error", error)
                return@addSnapshotListener
            }
            _campsites.clear()
            snapshot?.documents?.forEach { document ->
                runCatching {
                    val data = document.data
                    if (data != null) {
                        _campsites.add(documentToCampsite(document.id, data))
                    }
                }.onFailure { e ->
                    Log.e("CampsiteRepository", "Error parsing campsite ${document.id}", e)
                }
            }
        }
    }
}

