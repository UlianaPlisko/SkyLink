package com.skylink.backend.service.user

import com.skylink.backend.dto.user.ChangePasswordRequest
import com.skylink.backend.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserSecurityService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun changePassword(email: String, request: ChangePasswordRequest) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("User with email $email not found") }

        if (!passwordEncoder.matches(request.oldPassword, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect")
        }

        if (request.oldPassword == request.newPassword) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be the same as old password")
        }

        user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword))
        userRepository.save(user)
    }
}