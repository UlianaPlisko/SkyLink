package com.skylink.backend.model

import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class ConstStarConnectionId(
    val constellationId: Long = 0,
    val star1Id: Long = 0,
    val star2Id: Long = 0
) : Serializable