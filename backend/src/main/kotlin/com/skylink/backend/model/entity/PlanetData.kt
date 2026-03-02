package com.skylink.backend.model.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "\"PlanetData\"")
data class PlanetData(
    @Id
    val spaceObjectId: Long,

    @OneToOne
    @MapsId
    @JoinColumn(name = "space_object_id")
    val spaceObject: SpaceObject,

    @Column(name = "orbital_model", columnDefinition = "jsonb", nullable = false)
    var orbitalModel: String = "{}",   // JSON string for now

    @Column(name = "last_computed", nullable = false)
    val lastComputed: Instant
)