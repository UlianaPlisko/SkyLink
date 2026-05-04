package com.skylink.backend.model.entity

import com.skylink.backend.model.enums.EventSource
import com.skylink.backend.model.enums.EventType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "\"Events\"")
data class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false)
    val eventType: EventType,

    @Column(name = "start_at", nullable = false)
    val startAt: Instant,

    @Column(name = "end_at")
    val endAt: Instant? = null,

    @Column(name = "visibility_geom", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var visibilityGeom: String? = null,

    @Column(name = "location_geom", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var locationGeom: String? = null,

    @Column(name = "creator_id", nullable = false)
    val creatorId: Long,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    val source: EventSource = EventSource.USER,

    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: String = "{}",

    @Column(name = "chat_room_id")
    var chatRoomId: Long? = null,

    @Column(name = "max_capacity")
    var maxCapacity: Int? = null
)