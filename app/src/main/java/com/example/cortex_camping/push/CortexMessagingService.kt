package com.example.cortex_camping.push

import android.util.Log
import com.example.cortex_camping.util.DeviceUtils
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives pushes, and records where to send this device's pushes.
 *
 * Uses Firebase installation IDs. The older getToken/onNewToken API is deprecated and can't be
 * used alongside them.
 */
class CortexMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        val device = mapOf("fid" to installationId, "updatedAt" to FieldValue.serverTimestamp())
        Firebase.firestore.collection("devices").document(DeviceUtils.getDeviceId(this)).set(device)
            .addOnFailureListener { Log.e("CortexMessagingService", "Couldn't save this device's registration", it) }
    }

    // only called while the app is open, see PushNotifications.show
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        PushNotifications.show(this, notification.title.orEmpty(), notification.body.orEmpty(), message.data)
    }
}
