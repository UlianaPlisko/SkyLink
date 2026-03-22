package com.skylink.backend.service

import com.skylink.backend.model.entity.User
import com.skylink.backend.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User with id $userId not found") }
    }

    fun getByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("User with email $email not found") }
    }

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }
}