package com.example.cortex_camping.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cortex_camping.MainActivity
import com.example.cortex_camping.R
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.NotificationTarget

/**
 * Android's side of push notifications: the channel, showing one, and reading which one was
 * tapped. The data keys are set by functions/lib/push.js.
 */
object PushNotifications {
    private const val KEY_ID = "notificationId"
    private const val KEY_TARGET = "target"
    private const val KEY_CAMPSITE = "campsiteId"
    private const val KEY_SENDER = "senderId"
    private const val KEY_FOR_OWNER = "forOwner"

    /** Must exist before the first push arrives, or Android files it under a generic name. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            context.getString(R.string.notification_channel_id),
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canShow(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Shows a push that arrived while the app was open. Android shows the others by itself. */
    fun show(context: Context, title: String, body: String, data: Map<String, String>) {
        if (!canShow(context)) return
        val id = data[KEY_ID].orEmpty()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        data.forEach { (key, value) -> intent.putExtra(key, value) }
        val tap = PendingIntent.getActivity(
            context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, 0, notification)
        } catch (_: SecurityException) {
            // permission was taken away since the check
        }
    }

    /** The notification whose tap started [intent], or null if it was started some other way. */
    fun fromTap(intent: Intent?, deviceId: String): AppNotification? {
        // reopening the app from Recents repeats the old intent
        if (intent == null || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        val id = intent.getStringExtra(KEY_ID) ?: return null
        return AppNotification(
            id = id,
            recipientId = deviceId,
            senderId = intent.getStringExtra(KEY_SENDER).orEmpty(),
            campsiteId = intent.getStringExtra(KEY_CAMPSITE).orEmpty(),
            target = NotificationTarget.entries.firstOrNull { it.name == intent.getStringExtra(KEY_TARGET) }
                ?: NotificationTarget.NONE,
            forOwner = intent.getStringExtra(KEY_FOR_OWNER).toBoolean()
        )
    }
}
