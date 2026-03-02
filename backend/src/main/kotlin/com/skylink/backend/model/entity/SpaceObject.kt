package com.skylink.backend.model.entity

import com.skylink.backend.model.enums.SpaceObjectType
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "\"SpaceObject\"")
data class SpaceObject(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false)
    val objectType: SpaceObjectType,

    @Column(name = "ra_deg", nullable = false)
    val raDeg: Double,

    @Column(name = "dec_deg", nullable = false)
    val decDeg: Double,

    @Column(name = "magnitude")
    val magnitude: Double? = null,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null
)