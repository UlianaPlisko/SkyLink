package com.skylink.backend.model.entity

import com.skylink.backend.model.enums.ConstellationRegion
import jakarta.persistence.*

@Entity
@Table(name = "\"ConstellationCulture\"")
data class ConstellationCulture(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val region: ConstellationRegion,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "isCurrent", nullable = false)
    val isCurrent: Boolean = true
)