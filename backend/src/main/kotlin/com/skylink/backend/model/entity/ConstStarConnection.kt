package com.skylink.backend.model.entity

import com.skylink.backend.model.ConstStarConnectionId
import jakarta.persistence.*

@Entity
@Table(name = "\"ConstStarConnection\"")
data class ConstStarConnection(
    @EmbeddedId
    val id: ConstStarConnectionId = ConstStarConnectionId(),

    @ManyToOne
    @MapsId("constellationId")
    @JoinColumn(name = "constellation_id")
    val constellation: Constellation,

    @ManyToOne
    @MapsId("star1Id")
    @JoinColumn(name = "star1_id")
    val star1: SpaceObject,

    @ManyToOne
    @MapsId("star2Id")
    @JoinColumn(name = "star2_id")
    val star2: SpaceObject
)