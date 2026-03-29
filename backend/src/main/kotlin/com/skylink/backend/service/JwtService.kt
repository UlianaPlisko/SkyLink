package com.skylink.backend.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date

@Service
class JwtService(@Value("\${app.jwt.secret}") private val secret: String) {

    val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateToken(email: String): String =
        Jwts.builder()
            .setSubject(email)
            .claim("tokenType", "AUTH")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

    fun generatePendingGoogleToken(email: String, displayName: String): String =
        Jwts.builder()
            .setSubject(email)
            .claim("tokenType", "PENDING_GOOGLE")
            .claim("displayName", displayName)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000L * 60 * 10))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

    fun validatePendingToken(token: String): PendingGoogleClaims {
        val claims = extractAllClaims(token)
        if (claims["tokenType"] != "PENDING_GOOGLE")
            throw BadCredentialsException("Invalid token type")
        return PendingGoogleClaims(
            email = claims.subject,
            displayName = claims["displayName"] as String
        )
    }

    fun extractEmail(token: String): String =
        extractAllClaims(token).subject

    fun validateToken(token: String, email: String): Boolean {
        val claims = extractAllClaims(token)
        if (claims["tokenType"] != "AUTH") return false
        return claims.subject == email && !claims.expiration.before(Date())
    }

    private fun extractAllClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
}

data class PendingGoogleClaims(val email: String, val displayName: String)