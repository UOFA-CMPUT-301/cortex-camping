/*
 * NotificationRepository.kt
 *
 * Saves and loads notifications in the "notifications" collection on Firestore.
 *
 */

package com.example.cortex_camping.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.NotificationKind
import com.example.cortex_camping.model.NotificationTarget
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDateTime

class NotificationRepository {
    private val db = Firebase.firestore
    private val notificationsRef = db.collection("notifications")
    private val _notifications = mutableStateListOf<AppNotification>()

    val notifications: List<AppNotification>
        get() = _notifications

    /**
     * Saves a new notification. Firestore makes the id.
     *
     * @param notification the notification to save
     */
    fun send(notification: AppNotification) {
        val document = notificationsRef.document()
        document.set(toMap(notification.copy(id = document.id)))
    }

    /**
     * Marks notifications as read.
     *
     * @param ids the ids of the notifications to mark
     */
    fun markRead(ids: List<String>) {
        ids.forEach { id ->
            notificationsRef.document(id).update("read", true)
        }
    }

    init {
        notificationsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            _notifications.clear()
            snapshot?.documents?.forEach { document ->
                val notification = document.data?.let { fromMap(document.id, it) }
                if (notification != null) {
                    _notifications.add(notification)
                }
            }
        }
    }

    // converts a notification to a map so Firestore can save it
    private fun toMap(notification: AppNotification): Map<String, Any> {
        return mapOf(
            "id" to notification.id,
            "recipientId" to notification.recipientId,
            "senderId" to notification.senderId,
            "kind" to notification.kind.name,
            "title" to notification.title,
            "body" to notification.body,
            "link" to notification.link,
            "campsiteId" to notification.campsiteId,
            "target" to notification.target.name,
            "sentAt" to notification.sentAt.toString(),
            "read" to notification.read,
            "forOwner" to notification.forOwner
        )
    }

    // converts Firestore data back into a notification, null if something is missing
    private fun fromMap(id: String, data: Map<String, Any?>): AppNotification? {
        return try {
            AppNotification(
                id = id,
                recipientId = data["recipientId"] as? String ?: "",
                senderId = data["senderId"] as? String ?: "",
                kind = NotificationKind.valueOf(data["kind"] as String),
                title = data["title"] as? String ?: "",
                body = data["body"] as? String ?: "",
                link = data["link"] as? String ?: "",
                campsiteId = data["campsiteId"] as? String ?: "",
                target = NotificationTarget.valueOf(data["target"] as String),
                sentAt = LocalDateTime.parse(data["sentAt"] as String),
                read = data["read"] as? Boolean ?: false,
                forOwner = data["forOwner"] as? Boolean ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
}