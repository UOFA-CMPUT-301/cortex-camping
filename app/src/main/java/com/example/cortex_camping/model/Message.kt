package com.example.cortex_camping.model

import java.time.LocalDateTime

// a chat message between an entrant and the owner of a campsite
// each (campsiteId, entrantId) pair is one conversation
data class Message(
    val id: String = "",
    val campsiteId: String = "",
    val entrantId: String = "",
    val senderId: String = "",
    val text: String = "",
    val sentAt: LocalDateTime = LocalDateTime.now()
)
