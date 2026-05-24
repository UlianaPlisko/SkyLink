package com.codepalace.accelerometer

import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.config.ApiConfig
import com.codepalace.accelerometer.data.local.AppSettingsStorage
import com.codepalace.accelerometer.ui.theme.RedModeController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ApplicationAndApiClientTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteSharedPreferences("app_settings")
        context.deleteSharedPreferences("auth_prefs")
    }

    @Test
    fun `api config exposes backend base url`() {
        assertEquals("http://10.177.220.119:8080", ApiConfig.BASE_URL)
    }

    @Test
    fun `api client initializes session storage and retrofit services`() {
        ApiClient.init(context)

        assertNotNull(ApiClient.getSessionStorage())
        assertNotNull(ApiClient.celestialApi)
        assertNotNull(ApiClient.authApi)
        assertNotNull(ApiClient.weatherApi)
    }

    @Test
    fun `red mode controller saves and applies mode`() {
        RedModeController.setEnabled(context, true)

        assertTrue(AppSettingsStorage(context).redModeEnabled)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        AppSettingsStorage(context).redModeEnabled = false
        RedModeController.applySavedMode(context)

        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `application onCreate initializes services and notification channels`() {
        val app = ApplicationProvider.getApplicationContext<SkylinkApplication>()
        val notificationManager = app.getSystemService(NotificationManager::class.java)

        assertNotNull(ApiClient.getSessionStorage())
        assertEquals("Events & Reminders", notificationManager.getNotificationChannel("channel_events").name)
        assertEquals("Chat Messages", notificationManager.getNotificationChannel("channel_chat").name)
    }
}
