package com.skylink.backend.model.entity

import com.skylink.backend.model.enums.ObjectNameSource
import jakarta.persistence.*

@Entity
@Table(name = "\"ObjectName\"")
data class ObjectName(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "space_object_id", nullable = false)
    val spaceObject: SpaceObject,

    @Column(name = "name", nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    val source: ObjectNameSource,

    @Column(name = "is_primary", nullable = false)
    val isPrimary: Boolean = false
)