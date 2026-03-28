package com.skylink.backend.service.user

import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.dto.user.UserProfileResponse
import com.skylink.backend.model.entity.User
import com.skylink.backend.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserProfileService(
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

    fun getProfile(email: String): UserProfileResponse {
        val user = getByEmail(email)
        return toUserProfileResponse(user)
    }

    fun updateProfile(email: String, request: UpdateProfileRequest): UserProfileResponse {
        val user = getByEmail(email)
        request.displayName?.let { user.displayName = it }
        val updated = userRepository.save(user)
        return toUserProfileResponse(updated)
    }

    private fun toUserProfileResponse(user: User): UserProfileResponse {
        return UserProfileResponse(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
            role = user.role,
            createdAt = user.createdAt,
            lastUsedAt = user.lastUsedAt
        )
    }
}