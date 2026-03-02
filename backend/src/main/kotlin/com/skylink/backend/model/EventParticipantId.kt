package com.skylink.backend.model

import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class EventParticipantId(
    val eventId: Long = 0,
    val userId: Long = 0
) : Serializable