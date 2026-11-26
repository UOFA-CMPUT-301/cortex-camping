package com.example.cortex_camping

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.navigation.CortexApp
import com.example.cortex_camping.push.PushNotifications
import com.example.cortex_camping.ui.theme.CortexTheme
import com.example.cortex_camping.util.DeviceUtils
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    // the push notification the user tapped to open the app, until the app has shown it
    private var tappedNotification by mutableStateOf<AppNotification?>(null)

    // the app works without notifications, so the answer isn't needed
    private val askToNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setUpPush()
        // a recreated activity has already handled the tap that started it
        if (savedInstanceState == null) tappedNotification = PushNotifications.fromTap(intent, DeviceUtils.getDeviceId(this))
        setContent {
            CortexTheme {
                CortexApp(tappedNotification = tappedNotification, onTappedNotificationShown = { tappedNotification = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushNotifications.fromTap(intent, DeviceUtils.getDeviceId(this))?.let { tappedNotification = it }
    }

    private fun setUpPush() {
        PushNotifications.createChannel(this)
        // CortexMessagingService.onRegistered saves where to send this device's pushes
        FirebaseMessaging.getInstance().register()
            .addOnFailureListener { Log.e("MainActivity", "Couldn't register for push notifications", it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PushNotifications.canShow(this)) {
            askToNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
