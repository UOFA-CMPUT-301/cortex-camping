package com.example.cortex_camping.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cortex_camping.ui.screens.admin.ManagedUser
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore

/**
 * Observes camper and owner profiles and performs Firestore moderation operations.
 *
 * Profile IDs come from document paths. Deletion does not cascade into related records.
 * Page access is handled by the caller; this repository performs no role checks.
 * The owning ViewModel must call [close] to release profile listeners.
 */
class AdminModerationRepository {
    private val db = Firebase.firestore
    private val entrants = mutableStateListOf<ManagedUser>()
    private val owners = mutableStateListOf<ManagedUser>()
    private val userListeners = mutableListOf<ListenerRegistration>()
    private var generation = 0
    private var closed = false

    /** Most recent profile-list loading error, exposed for UI feedback. */
    var error by mutableStateOf<String?>(null)
        private set

    /** Profiles sorted by name; a device with both roles appears as two separate entries. */
    val users: List<ManagedUser>
        get() = (entrants + owners).sortedBy { it.name.lowercase() }

    init {
        startUsers()
    }

    /**
     * Subscribes to both profile collections and maps their documents to display rows.
     * Callbacks from stopped subscriptions are ignored using a generation counter.
     */
    private fun startUsers() {
        val currentGeneration = ++generation
        listOf("entrants" to entrants, "owners" to owners).forEach { (collection, target) ->
            userListeners += db.collection(collection).addSnapshotListener { snapshot, failure ->
                if (closed || currentGeneration != generation) return@addSnapshotListener
                target.clear()
                if (failure != null) {
                    error = "Couldn't load user profiles. Please reopen the app to retry."
                    return@addSnapshotListener
                }
                target.addAll(snapshot?.documents.orEmpty().map { document ->
                    ManagedUser(
                        id = document.id,
                        name = (document.get("name") as? String).orEmpty().ifBlank { "Unnamed user" },
                        email = (document.get("email") as? String).orEmpty(),
                        summary = if (collection == "owners") "Campsite owner" else "Camper",
                        isOwner = collection == "owners"
                    )
                })
            }
        }
    }

    /**
     * Deletes only the profile selected by [user.isOwner] and [user.id].
     * Other profiles, messages, campsites, and authentication accounts are retained.
     *
     * @param onResult Receives null after server acknowledgement, or an error description.
     * Validation failures are reported immediately.
     */
    fun deleteUser(user: ManagedUser, onResult: (String?) -> Unit) =
        deleteDocument(if (user.isOwner) "owners" else "entrants", user.id, onResult)

    /**
     * Deletes the campsite document identified by [id], without cascading to related records.
     *
     * @param onResult Receives null after server acknowledgement, or an error description.
     */
    fun deleteCampsite(id: String, onResult: (String?) -> Unit) =
        deleteDocument("campsite", id, onResult)

    /** Validates the document ID, deletes the record, and forwards the write result. */
    private fun deleteDocument(collection: String, id: String, onResult: (String?) -> Unit) {
        if (!validateRecord(id, onResult)) return
        val target = db.collection(collection).document(id)
        target.delete().addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it.message ?: "Deletion failed. Please retry.") }
    }

    /**
     * Sets the campsite's imageUrl field to null while preserving its other fields.
     *
     * The transaction fails if the campsite is missing or its image differs from
     * [expectedImage], so an image replaced since review is not removed accidentally.
     * External image files are not deleted.
     *
     * @param id Firestore campsite document ID.
     * @param expectedImage Image value shown when the user confirmed removal.
     * @param onResult Receives null after commit, or a validation/transaction error.
     */
    fun deleteImage(id: String, expectedImage: String?, onResult: (String?) -> Unit) {
        if (!validateRecord(id, onResult)) return
        val target = db.collection("campsite").document(id)
        db.runTransaction { transaction ->
            val campsite = transaction.get(target)
            check(campsite.exists()) { "This campsite no longer exists." }
            check(campsite.get("imageUrl") == expectedImage) {
                "The image changed. Review the new image before deleting it."
            }
            transaction.update(target, "imageUrl", null)
            null
        }.addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it.message ?: "Image deletion failed. Please retry.") }
    }

    /** Returns false and reports an error if the repository is closed or [id] is invalid. */
    private fun validateRecord(id: String, onResult: (String?) -> Unit): Boolean {
        if (closed) {
            onResult("This screen has closed. Please reopen it and retry.")
            return false
        }
        if (id.isBlank() || '/' in id) {
            onResult("Invalid record ID.")
            return false
        }
        return true
    }

    /** Invalidates queued callbacks, removes profile listeners, and clears cached profiles. */
    private fun stopUsers() {
        generation++
        userListeners.forEach { it.remove() }
        userListeners.clear()
        entrants.clear()
        owners.clear()
    }

    /** Releases profile listeners and prevents further mutations through this instance. */
    fun close() {
        closed = true
        stopUsers()
    }
}
