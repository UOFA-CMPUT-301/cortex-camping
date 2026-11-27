package com.example.cortex_camping.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Stores campsite photos in Firebase Storage under `campsites/{campsiteId}/` and profile pictures
 * under `entrants/{entrantId}/`.
 */
class CampsiteImageRepository {
    // The bucket is named explicitly so an older google-services.json without one still works
    private val storage = Firebase.storage(BUCKET).apply {
        // the defaults leave the user waiting for up to 10 minutes when offline
        maxUploadRetryTimeMillis = RETRY_LIMIT_MS
        maxOperationRetryTimeMillis = RETRY_LIMIT_MS
    }

    /**
     * Uploads a JPEG under a new name and returns its download URL. Names are never reused, so a
     * replaced photo always has a different URL and can't be confused with a cached one.
     */
    suspend fun upload(campsiteId: String, jpeg: ByteArray): String = uploadTo("campsites/$campsiteId", jpeg)

    /** Uploads a profile picture and returns its download URL. */
    suspend fun uploadProfileImage(entrantId: String, jpeg: ByteArray): String = uploadTo("entrants/$entrantId", jpeg)

    private suspend fun uploadTo(folder: String, jpeg: ByteArray): String {
        val ref = storage.reference.child("$folder/${UUID.randomUUID()}.jpg")
        val metadata = storageMetadata {
            contentType = "image/jpeg"
            cacheControl = "private, max-age=31536000"
        }
        ref.putBytes(jpeg, metadata).await()
        return try {
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            ref.delete()
            throw e
        }
    }

    /**
     * Deletes the photo behind a download URL. A failure only leaves an unused file behind, so it
     * is logged rather than reported. Values that aren't files in this bucket are ignored.
     */
    fun delete(imageUrl: String?) {
        if (imageUrl == null || !isStorageUrl(imageUrl)) return
        runCatching { storage.getReferenceFromUrl(imageUrl) }
            .onSuccess { ref ->
                ref.delete().addOnFailureListener { Log.w(TAG, "Couldn't delete ${ref.path}", it) }
            }
            .onFailure { Log.w(TAG, "Not a file in $BUCKET", it) }
    }

    private fun isStorageUrl(url: String) =
        url.startsWith("https://firebasestorage.googleapis.com") || url.startsWith("gs://")

    private companion object {
        const val TAG = "CampsiteImageRepository"
        const val BUCKET = "gs://cortex-camping-cmput301.firebasestorage.app"
        const val RETRY_LIMIT_MS = 30_000L
    }
}
