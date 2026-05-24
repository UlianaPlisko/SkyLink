package com.codepalace.accelerometer.testing

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Base64
import android.view.View
import android.widget.Checkable
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppSettingsStorage
import org.hamcrest.Matcher
import java.nio.charset.StandardCharsets

object AndroidTestSupport {

    fun clearAppState(context: Context) {
        ApiClient.overrideBaseUrlForTests(null)
        context.deleteSharedPreferences("auth_prefs")
        context.deleteSharedPreferences("app_settings")
        context.deleteSharedPreferences("fcm_prefs")
    }

    fun prepareSkyViewLaunch(context: Context) {
        AppSettingsStorage(context).autoLocationEnabled = false
        grantNotificationPermissionIfNeeded(context)
    }

    fun waitUntilDisplayed(viewId: Int, timeoutMillis: Long = 5_000L) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Throwable? = null

        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(100)
            }
        }

        throw AssertionError("View $viewId was not displayed within ${timeoutMillis}ms", lastError)
    }

    fun setChecked(checked: Boolean): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun getDescription(): String = "set checked state to $checked"

            override fun perform(uiController: UiController, view: View) {
                val checkable = view as Checkable
                if (checkable.isChecked != checked) {
                    checkable.isChecked = checked
                }
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    private fun grantNotificationPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                InstrumentationRegistry.getInstrumentation()
                    .uiAutomation
                    .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

object TestTokens {

    fun authJwt(expiresInSeconds: Long = 3_600L): String {
        return jwt(tokenType = "AUTH", expiresInSeconds = expiresInSeconds)
    }

    private fun jwt(tokenType: String, expiresInSeconds: Long): String {
        val exp = System.currentTimeMillis() / 1000L + expiresInSeconds
        val payload = """{"tokenType":"$tokenType","exp":$exp}"""
        val encodedPayload = Base64.encodeToString(
            payload.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        return "header.$encodedPayload.signature"
    }
}
