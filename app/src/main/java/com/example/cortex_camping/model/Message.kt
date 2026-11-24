package com.example.cortex_camping.model

import java.time.LocalDateTime

/**
 * A chat message between a camper and a campsite owner.
 *
 * [campsiteId] and [entrantId] together identify the conversation; [senderId] identifies
 * its author. [sentAt] is a local date-time for display, converted from the server timestamp
 * by the repository (or read directly from a legacy local-time string).
 */
data class Message(
    val id: String = "",
    val campsiteId: String = "",
    val entrantId: String = "",
    val senderId: String = "",
    val text: String = "",
    val sentAt: LocalDateTime = LocalDateTime.now()
)
