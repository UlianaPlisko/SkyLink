package com.skylink.backend.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "\"UserCredentials\"")
data class UserCredentials(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String
)