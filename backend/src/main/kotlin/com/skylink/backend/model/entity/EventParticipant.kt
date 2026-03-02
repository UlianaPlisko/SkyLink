package com.skylink.backend.model.entity

import com.skylink.backend.model.EventParticipantId
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "\"EventParticipant\"")
data class EventParticipant(
    @EmbeddedId
    val id: EventParticipantId = EventParticipantId(),

    @ManyToOne
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    val event: Event,

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "enrolled_at", nullable = false)
    val enrolledAt: Instant = Instant.now()
)