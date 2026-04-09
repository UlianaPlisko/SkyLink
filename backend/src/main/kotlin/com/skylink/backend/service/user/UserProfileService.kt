package com.skylink.backend.service.user

import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.dto.user.UserProfileResponse
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserPfp
import com.skylink.backend.repository.UserPfpRepository
import com.skylink.backend.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

data class PfpFile(
    val bytes: ByteArray,
    val contentType: String
)

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val userPfpRepository: UserPfpRepository
) {

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
        )

        private const val MAX_PFP_SIZE_BYTES = 5 * 1024 * 1024
        private const val PFP_URL = "/api/profile/pfp"
    }

    @Transactional(readOnly = true)
    fun getById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "User with id $userId not found")
            }
    }

    @Transactional(readOnly = true)
    fun getByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "User with email $email not found")
            }
    }

    @Transactional(readOnly = true)
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    @Transactional(readOnly = true)
    fun getProfile(email: String): UserProfileResponse {
        val user = getByEmail(email)
        return toUserProfileResponse(user)
    }

    @Transactional
    fun updateProfile(email: String, request: UpdateProfileRequest): UserProfileResponse {
        val user = getByEmail(email)
        request.displayName?.let { user.displayName = it }
        val updated = userRepository.save(user)
        return toUserProfileResponse(updated)
    }

    @Transactional
    fun updatePfp(email: String, file: MultipartFile) {
        val user = getByEmail(email)

        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        }

        val contentType = file.contentType?.lowercase()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File content type is missing")

        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only JPG, PNG, and WEBP are allowed"
            )
        }

        if (file.size > MAX_PFP_SIZE_BYTES) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "File size must not exceed 5 MB"
            )
        }

        val existingPfp = userPfpRepository.findById(user.id).orElse(null)

        if (existingPfp == null) {
            userPfpRepository.save(
                UserPfp(
                    userId = user.id,
                    data = file.bytes,
                    contentType = contentType
                )
            )
        } else {
            existingPfp.data = file.bytes
            existingPfp.contentType = contentType
            userPfpRepository.save(existingPfp)
        }
    }

    @Transactional(readOnly = true)
    fun getPfp(email: String): PfpFile? {
        val user = getByEmail(email)
        val pfp = userPfpRepository.findById(user.id).orElse(null) ?: return null

        return PfpFile(
            bytes = pfp.data,
            contentType = pfp.contentType
        )
    }

    @Transactional
    fun deletePfp(email: String): Boolean {
        val user = getByEmail(email)
        val pfp = userPfpRepository.findById(user.id).orElse(null) ?: return false

        userPfpRepository.delete(pfp)
        return true
    }

    private fun toUserProfileResponse(user: User): UserProfileResponse {
        val pfpExists = userPfpRepository.existsById(user.id)

        return UserProfileResponse(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
            role = user.role,
            createdAt = user.createdAt,
            lastUsedAt = user.lastUsedAt,
            pfpUrl = if (pfpExists) PFP_URL else null
        )
    }
}