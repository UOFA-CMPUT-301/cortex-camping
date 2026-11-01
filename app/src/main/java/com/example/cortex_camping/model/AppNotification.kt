package com.example.cortex_camping.model

import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.LocalDateTime

// decides the icon and colour of the notification row
enum class NotificationKind {
    BOOKING_OFFER,
    MESSAGE,
    LOTTERY_UPDATED,
    SUCCESS,
    CANCELLED,
    NEUTRAL,
    REVIEW,
    ALERT
}

// where tapping the notification's link takes the user
enum class NotificationTarget {
    CHOOSE_DATES,
    CHAT,
    CAMPSITE_LOTTERY,
    MY_RESERVATIONS,
    MY_LOTTERIES,
    BROWSE,
    OWNER_RESERVATIONS,
    OWNER_LOTTERIES,
    OWNER_CAMPSITES,
    NONE
}

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "", // empty when the app sends it, e.g. lottery results
    val kind: NotificationKind = NotificationKind.NEUTRAL,
    val title: String = "",
    val body: String = "",
    val link: String = "", // the call to action line, e.g. "View booking →"
    val campsiteId: String = "",
    val target: NotificationTarget = NotificationTarget.NONE,
    val sentAt: LocalDateTime = LocalDateTime.now(),
    val read: Boolean = false,
    // one device can be both an entrant and an owner, so this says which feed it belongs in
    val forOwner: Boolean = false
) {
    @get:Exclude
    val isToday: Boolean
        get() = sentAt.toLocalDate() == LocalDate.now()
}
