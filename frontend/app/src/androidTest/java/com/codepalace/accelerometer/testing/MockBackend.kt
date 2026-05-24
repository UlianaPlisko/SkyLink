package com.codepalace.accelerometer.testing

import android.content.Context
import com.codepalace.accelerometer.api.ApiClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

class MockBackend(private val context: Context) {

    private var server: MockWebServer? = null

    fun start() {
        stop()
        ApiClient.init(context)
        AndroidTestSupport.prepareSkyViewLaunch(context)

        server = MockWebServer().apply {
            dispatcher = SkyLinkDispatcher
            start()
        }

        ApiClient.overrideBaseUrlForTests(server!!.url("/").toString())
    }

    fun stop() {
        ApiClient.overrideBaseUrlForTests(null)
        server?.shutdown()
        server = null
    }

    private object SkyLinkDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return when (request.path?.substringBefore("?")) {
                "/api/auth/login" -> jsonResponse(
                    """
                    {
                      "token": "${TestTokens.authJwt()}",
                      "role": "OBSERVER",
                      "displayName": "Mock User",
                      "userId": 42
                    }
                    """.trimIndent()
                )

                "/api/celestial/space-objects" -> jsonResponse("[]")

                "/api/weather/current-visibility" -> jsonResponse(
                    """
                    {
                      "location": "Bratislava",
                      "localTime": "2026-05-24T20:00:00",
                      "visibilityKm": 10.0,
                      "cloudPercent": 5,
                      "humidity": 45,
                      "precipitationMm": 0.0,
                      "conditionText": "Clear",
                      "conditionCode": 1000,
                      "day": false
                    }
                    """.trimIndent()
                )

                else -> MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"message":"Mock endpoint is not configured."}""")
            }
        }

        private fun jsonResponse(body: String): MockResponse {
            return MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        }
    }
}
