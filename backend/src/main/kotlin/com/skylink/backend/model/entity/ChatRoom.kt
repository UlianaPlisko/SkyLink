package com.skylink.backend.model.entity

import com.skylink.backend.model.enums.ChatRoomType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "\"ChatRoom\"")
data class ChatRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    var type: ChatRoomType,

    @Column(name = "region_geom", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var regionGeom: String? = null,

    @Column(name = "event_id")
    var eventId: Long? = null,

    @Column(name = "created_by")
    var createdBy: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)