package com.skylink.backend.model.entity

import com.skylink.backend.model.enums.DeepSkyObjectClass
import jakarta.persistence.*

@Entity
@Table(name = "\"DeepSkyObjectData\"")
data class DeepSkyObjectData(
    @Id
    val spaceObjectId: Long,

    @OneToOne
    @MapsId
    @JoinColumn(name = "space_object_id")
    val spaceObject: SpaceObject,

    @Column(name = "catalog_id")
    val catalogId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "object_class", nullable = false)
    val objectClass: DeepSkyObjectClass,

    @Column(name = "angular_size")
    val angularSize: Double? = null
)