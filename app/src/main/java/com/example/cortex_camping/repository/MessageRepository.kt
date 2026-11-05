package com.example.cortex_camping.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.Message
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDateTime

class MessageRepository {
    private val db = Firebase.firestore
    private val messagesRef = db.collection("messages")
    private val _messages = mutableStateListOf<Message>()

    val messages: List<Message>
        get() = _messages

    init {
        messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            _messages.clear()
            snapshot?.documents?.forEach { document ->
                document.data?.let { data ->
                    documentToMessage(document.id, data)?.let(_messages::add)
                }
            }
            _messages.sortBy { it.sentAt }
        }
    }

    fun conversation(campsiteId: String, entrantId: String): List<Message> {
        return _messages.filter {
            it.campsiteId == campsiteId && it.entrantId == entrantId
        }.sortedBy { it.sentAt }
    }

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

    private fun messageToMap(message: Message): Map<String, Any> {
        return mapOf(
            "id" to message.id,
            "campsiteId" to message.campsiteId,
            "entrantId" to message.entrantId,
            "senderId" to message.senderId,
            "text" to message.text,
            "sentAt" to message.sentAt.toString()
        )
    }

    private fun documentToMessage(id: String, data: Map<String, Any?>): Message? {
        return try {
            Message(
                id = (data["id"] as? String)?.ifBlank { id } ?: id,
                campsiteId = data["campsiteId"] as? String ?: "",
                entrantId = data["entrantId"] as? String ?: "",
                senderId = data["senderId"] as? String ?: "",
                text = data["text"] as? String ?: "",
                sentAt = LocalDateTime.parse(
                    data["sentAt"] as? String ?: LocalDateTime.now().toString()
                )
            )
        } catch (_: Exception) {
            null
        }
    }
}
