package com.skylink.backend.model.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "\"Favorite\"")
data class Favorite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne
    @JoinColumn(name = "space_object_id", nullable = false)
    val spaceObject: SpaceObject,

    @Column(columnDefinition = "TEXT")
    var note: String? = null,

    @Column
    var visibility: Double = 1.0,

    @Column(name = "added_at", nullable = false)
    val addedAt: Instant = Instant.now()
)