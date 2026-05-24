package com.codepalace.accelerometer.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.codepalace.accelerometer.data.local.SessionStorage
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AuthInterceptorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sessionStorage = SessionStorage(context)

    @After
    fun tearDown() {
        context.deleteSharedPreferences("auth_prefs")
    }

    @Test
    fun `adds bearer token when session contains valid token`() {
        val token = jwt("AUTH", expiresInSeconds = 3600)
        sessionStorage.saveAuth(token, "OBSERVER", "Alice", 1L)
        val chain = RecordingChain(responseCode = 200)

        AuthInterceptor(sessionStorage).intercept(chain)

        assertEquals("Bearer $token", chain.proceededRequest.header("Authorization"))
        assertEquals(token, sessionStorage.getToken())
    }

    @Test
    fun `does not add authorization header without token`() {
        val chain = RecordingChain(responseCode = 200)

        AuthInterceptor(sessionStorage).intercept(chain)

        assertNull(chain.proceededRequest.header("Authorization"))
    }

    @Test
    fun `clears auth when authenticated request receives unauthorized response`() {
        val token = jwt("AUTH", expiresInSeconds = 3600)
        sessionStorage.saveAuth(token, "OBSERVER", "Alice", 1L)
        val chain = RecordingChain(responseCode = 401)

        AuthInterceptor(sessionStorage).intercept(chain)

        assertNull(sessionStorage.getToken())
    }

    private fun jwt(tokenType: String, expiresInSeconds: Long): String {
        val exp = System.currentTimeMillis() / 1000L + expiresInSeconds
        val payload = """{"tokenType":"$tokenType","exp":$exp}"""
        val encodedPayload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray())
        return "header.$encodedPayload.signature"
    }

    private class RecordingChain(
        private val responseCode: Int
    ) : Interceptor.Chain {

        private val originalRequest = Request.Builder()
            .url("http://localhost/test")
            .build()

        lateinit var proceededRequest: Request

        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(responseCode)
                .message("test")
                .build()
        }

        override fun connection(): Connection? = null

        override fun call(): Call = throw UnsupportedOperationException()

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
