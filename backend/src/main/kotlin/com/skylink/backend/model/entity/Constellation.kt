package com.skylink.backend.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "\"Constellation\"")
data class Constellation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "culture_id", nullable = false)
    val culture: ConstellationCulture,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)