package com.skylink.backend.service

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.BadCredentialsException
import java.util.Date

class JwtServiceTest {

    private val service = JwtService("01234567890123456789012345678901")

    @Test
    fun `auth token can be generated extracted and validated`() {
        val token = service.generateToken("user@test.com")

        assertEquals("user@test.com", service.extractEmail(token))
        assertTrue(service.validateToken(token, "user@test.com"))
        assertFalse(service.validateToken(token, "other@test.com"))
    }

    @Test
    fun `pending google token can be generated and validated`() {
        val token = service.generatePendingGoogleToken("google@test.com", "Google User")

        val claims = service.validatePendingToken(token)

        assertEquals("google@test.com", claims.email)
        assertEquals("Google User", claims.displayName)
        assertFalse(service.validateToken(token, "google@test.com"))
    }

    @Test
    fun `pending validation rejects auth token type`() {
        val token = service.generateToken("user@test.com")

        assertThrows<BadCredentialsException> {
            service.validatePendingToken(token)
        }
    }

    @Test
    fun `expired auth token is rejected`() {
        val now = System.currentTimeMillis()
        val expiredToken = Jwts.builder()
            .setSubject("user@test.com")
            .claim("tokenType", "AUTH")
            .setIssuedAt(Date(now - 2_000))
            .setExpiration(Date(now - 1_000))
            .signWith(service.key, SignatureAlgorithm.HS256)
            .compact()

        assertThrows<ExpiredJwtException> {
            service.validateToken(expiredToken, "user@test.com")
        }
    }
}
