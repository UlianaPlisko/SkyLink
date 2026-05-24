package com.skylink.backend.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.server.ResponseStatusException
import java.lang.reflect.Method

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handles not found and unauthorized exceptions`() {
        val notFound = handler.handleNotFound(ResourceNotFoundException("missing"))
        val unauthorized = handler.handleUnauthorized(UnauthorizedException("denied"))
        val defaultNotFoundException: ResourceNotFoundException = mock()
        val defaultUnauthorizedException: UnauthorizedException = mock()
        whenever(defaultNotFoundException.message).thenReturn(null)
        whenever(defaultUnauthorizedException.message).thenReturn(null)
        val defaultNotFound = handler.handleNotFound(defaultNotFoundException)
        val defaultUnauthorized = handler.handleUnauthorized(defaultUnauthorizedException)

        assertEquals(HttpStatus.NOT_FOUND, notFound.statusCode)
        assertEquals("missing", notFound.body?.message)
        assertEquals(HttpStatus.FORBIDDEN, unauthorized.statusCode)
        assertEquals("denied", unauthorized.body?.message)
        assertEquals("Resource not found", defaultNotFound.body?.message)
        assertEquals("Access denied", defaultUnauthorized.body?.message)
    }

    @Test
    fun `handles validation errors`() {
        val bindingResult: BindingResult = mock()
        val method: Method = GlobalExceptionHandlerTest::class.java.getDeclaredMethod("sample", String::class.java)
        whenever(bindingResult.fieldErrors).thenReturn(listOf(FieldError("request", "email", "must be valid")))
        val exception = MethodArgumentNotValidException(MethodParameter(method, 0), bindingResult)

        val response = handler.handleValidation(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("email: must be valid", response.body?.message)
    }

    @Test
    fun `handles bad credentials and response status exceptions`() {
        val badCredentials = handler.handleBadCredentials(BadCredentialsException("bad"))
        val explicitStatus = handler.handleResponseStatus(ResponseStatusException(HttpStatus.NOT_FOUND, "gone"))
        val fallbackStatus = handler.handleResponseStatus(ResponseStatusException(HttpStatusCode.valueOf(499), null))

        assertEquals(HttpStatus.UNAUTHORIZED, badCredentials.statusCode)
        assertEquals("bad", badCredentials.body?.message)
        assertEquals(HttpStatus.NOT_FOUND, explicitStatus.statusCode)
        assertEquals("gone", explicitStatus.body?.message)
        assertEquals(HttpStatus.BAD_REQUEST, fallbackStatus.statusCode)
    }

    @Test
    fun `handles illegal argument and illegal state with conflict detection`() {
        val badArgument = handler.handleIllegalArgument(IllegalArgumentException("bad input"))
        val conflictArgument = handler.handleIllegalArgument(IllegalArgumentException("already exists"))
        val badState = handler.handleIllegalState(IllegalStateException("bad state"))
        val conflictState = handler.handleIllegalState(IllegalStateException("already registered"))
        val defaultBadCredentials = handler.handleBadCredentials(BadCredentialsException(null as String?))
        val defaultResponseStatus = handler.handleResponseStatus(ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        val defaultArgument = handler.handleIllegalArgument(IllegalArgumentException())
        val defaultState = handler.handleIllegalState(IllegalStateException())

        assertEquals(HttpStatus.BAD_REQUEST, badArgument.statusCode)
        assertEquals(HttpStatus.CONFLICT, conflictArgument.statusCode)
        assertEquals(HttpStatus.BAD_REQUEST, badState.statusCode)
        assertEquals(HttpStatus.CONFLICT, conflictState.statusCode)
        assertEquals("Invalid credentials", defaultBadCredentials.body?.message)
        assertEquals(HttpStatus.I_AM_A_TEAPOT.reasonPhrase, defaultResponseStatus.body?.message)
        assertEquals(HttpStatus.BAD_REQUEST.reasonPhrase, defaultArgument.body?.message)
        assertEquals(HttpStatus.BAD_REQUEST.reasonPhrase, defaultState.body?.message)
    }

    @Suppress("unused")
    private fun sample(value: String) = value
}
