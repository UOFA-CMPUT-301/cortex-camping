package com.example.cortex_camping.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.Message
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import java.time.ZoneId
import com.google.firebase.firestore.firestore
import java.time.LocalDateTime

/**
 * Maintains a Compose-observable cache of the Firestore messages collection.
 *
 * Conversations are filtered locally by campsite and entrant. New writes use server
 * timestamps; pending writes use estimated timestamps until the server acknowledges them.
 * The collection listener currently remains attached for the lifetime of this instance.
 */
class MessageRepository {
    private val db = Firebase.firestore
    private val messagesRef = db.collection("messages")
    private val _messages = mutableStateListOf<Message>()

    /** Cached messages ordered by their display-local send time, oldest first. */
    val messages: List<Message>
        get() = _messages

    init {
        messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            _messages.clear()
            snapshot?.documents?.forEach { document ->
                document.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { data ->
                    documentToMessage(document.id, data)?.let(_messages::add)
                }
            }
            _messages.sortBy { it.sentAt }
        }
    }

    /**
     * Returns cached messages for the exact [campsiteId]/[entrantId] conversation.
     * This filters the local cache and does not issue a scoped Firestore query.
     */
    fun conversation(campsiteId: String, entrantId: String): List<Message> {
        return _messages.filter {
            it.campsiteId == campsiteId && it.entrantId == entrantId
        }.sortedBy { it.sentAt }
    }

    /**
     * Writes a new message using an automatically generated document ID.
     *
     * Blank participant/campsite IDs and whitespace-only text are ignored. Text is trimmed
     * before storage. The snapshot listener supplies the displayed message; this method
     * does not report write completion or errors and does not send a notification.
     */
    fun sendMessage(campsiteId: String, entrantId: String, senderId: String, text: String) {
        if (campsiteId.isBlank() || entrantId.isBlank() || senderId.isBlank()) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val document = messagesRef.document()
        val message = Message(
            id = document.id,
            campsiteId = campsiteId,
            entrantId = entrantId,
            senderId = senderId,
            text = trimmed,
            sentAt = LocalDateTime.now()
        )
        document.set(messageToMap(message))
    }

    /** Serializes a message, replacing its local sentAt value with a server timestamp. */
    private fun messageToMap(message: Message): Map<String, Any> {
        return mapOf(
            "id" to message.id,
            "campsiteId" to message.campsiteId,
            "entrantId" to message.entrantId,
            "senderId" to message.senderId,
            "text" to message.text,
            "sentAt" to FieldValue.serverTimestamp()
        )
    }

    /**
     * Decodes a Firestore timestamp into the device's local timezone.
     *
     * Legacy ISO local-date-time strings remain readable without timezone conversion.
     * Missing, unsupported, or malformed timestamps cause the document to be skipped.
     */
    private fun documentToMessage(id: String, data: Map<String, Any?>): Message? {
        return try {
            Message(
                id = (data["id"] as? String)?.ifBlank { id } ?: id,
                campsiteId = data["campsiteId"] as? String ?: "",
                entrantId = data["entrantId"] as? String ?: "",
                senderId = data["senderId"] as? String ?: "",
                text = data["text"] as? String ?: "",
                sentAt = when (val timestamp = data["sentAt"]) {
                    is Timestamp -> LocalDateTime.ofInstant(
                        timestamp.toDate().toInstant(), ZoneId.systemDefault()
                    )
                    // Old documents contain local times with no timezone information.
                    is String -> LocalDateTime.parse(timestamp)
                    else -> return null
                }
            )
        } catch (_: Exception) {
            null
        }
    }
}
