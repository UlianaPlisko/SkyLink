package com.skylink.backend.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.skylink.backend.dto.auth.*
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserCredentials
import com.skylink.backend.model.enums.AuthProvider
import com.skylink.backend.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val googleVerifier: GoogleIdTokenVerifier
) : AuthServiceInterface {

    override fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email))
            throw IllegalArgumentException("Email already in use")

        val user = User(
            email = request.email,
            displayName = request.displayName,
            role = request.role,
            provider = AuthProvider.LOCAL
        )

        val credential = UserCredentials(
            user = user,
            passwordHash = requireNotNull(passwordEncoder.encode(request.password))
        )
        user.localCredential = credential

        userRepository.save(user)
        return user.toAuthResponse(jwtService)
    }

    override fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BadCredentialsException("User not found") }

        if (user.provider != AuthProvider.LOCAL)
            throw BadCredentialsException("Please sign in with Google")

        val credential = requireNotNull(user.localCredential) {
            "Local credential missing"
        }

        if (!passwordEncoder.matches(request.password, credential.passwordHash))
            throw BadCredentialsException("Invalid password")

        user.lastUsedAt = Instant.now()
        userRepository.save(user)

        return user.toAuthResponse(jwtService)
    }

    override fun googleAuth(request: GoogleAuthRequest): GoogleCallbackResponse {
        val payload = googleVerifier.verify(request.idToken)?.payload
            ?: throw BadCredentialsException("Invalid Google token")

        val email = payload.email ?: throw BadCredentialsException("Google token has no email")
        val displayName = payload["name"] as? String ?: "User"

        val existingUser = userRepository.findByEmail(email).orElse(null)

        if (existingUser != null) {
            if (existingUser.provider != AuthProvider.GOOGLE)
                throw BadCredentialsException("Email registered with password login")

            existingUser.lastUsedAt = Instant.now()
            userRepository.save(existingUser)

            return GoogleCallbackResponse(
                token = jwtService.generateToken(existingUser.email),
                isPending = false
            )
        }

        return GoogleCallbackResponse(
            token = jwtService.generatePendingGoogleToken(email, displayName),
            isPending = true,
            displayName = displayName
        )
    }

    override fun completeGoogleRegistration(request: CompleteGoogleRequest): AuthResponse {
        val claims = jwtService.validatePendingToken(request.pendingToken)

        if (userRepository.existsByEmail(claims.email))
            throw IllegalStateException("User already registered")

        val user = User(
            email = claims.email,
            displayName = claims.displayName,
            role = request.role,
            provider = AuthProvider.GOOGLE
        )

        try {
            userRepository.save(user)
        } catch (ex: DataIntegrityViolationException) {
            throw IllegalStateException("User already registered")
        }

        return user.toAuthResponse(jwtService)
    }
}

private fun User.toAuthResponse(jwtService: JwtService) = AuthResponse(
    token = jwtService.generateToken(email),
    role = role,
    displayName = displayName,
    userId = id
)