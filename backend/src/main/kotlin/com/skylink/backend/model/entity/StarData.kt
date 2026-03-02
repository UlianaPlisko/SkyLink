package com.skylink.backend.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "\"StarData\"")
data class StarData(
    @Id
    val spaceObjectId: Long,

    @OneToOne
    @MapsId
    @JoinColumn(name = "space_object_id")
    val spaceObject: SpaceObject,

    @Column(name = "ra_j2000", nullable = false)
    val raJ2000: Double,

    @Column(name = "pm_ra")
    val pmRa: Double? = null,

    @Column(name = "pm_dec")
    val pmDec: Double? = null,

    @Column(name = "bt_vt")
    val btVt: Double? = null,

    @Column(name = "constellation", length = 3)
    val constellation: String? = null,

    @Column(name = "spectral_type", length = 20)
    val spectralType: String? = null,

    @Column(name = "distance_ly")
    val distanceLy: Double? = null
)