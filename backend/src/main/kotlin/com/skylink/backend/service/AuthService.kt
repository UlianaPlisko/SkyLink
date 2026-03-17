package com.skylink.backend.service

import com.skylink.backend.dto.auth.*
import com.skylink.backend.model.entity.User
import com.skylink.backend.repository.UserRepository
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) : AuthServiceInterface {

    override fun register(request: RegisterRequest): AuthResponse {

        val user = User(
            email = request.email,
            displayName = request.displayName,
            passwordHash = requireNotNull(passwordEncoder.encode(request.password)) { "Password encoding failed" },
            role = request.role
        )

        val savedUser = userRepository.save(user)

        val token = jwtService.generateToken(savedUser.email)

        return AuthResponse(
            token = token,
            role = savedUser.role,
            displayName = savedUser.displayName,
            userId = savedUser.id
        )
    }

    override fun login(request: LoginRequest): AuthResponse {

        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BadCredentialsException("User not found") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BadCredentialsException("Invalid password")
        }

        val token = jwtService.generateToken(user.email)

        return AuthResponse(
            token = token,
            role = user.role,
            displayName = user.displayName,
            userId = user.id
        )
    }
}