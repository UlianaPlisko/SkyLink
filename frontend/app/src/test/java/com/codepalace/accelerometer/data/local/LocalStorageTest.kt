package com.codepalace.accelerometer.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalStorageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteSharedPreferences("auth_prefs")
        context.deleteSharedPreferences("app_settings")
        context.filesDir.resolve("profile_images").deleteRecursively()
    }

    @Test
    fun `app settings expose defaults and store valid preferences`() {
        val storage = AppSettingsStorage(context)

        assertFalse(storage.notificationsEnabled)
        assertFalse(storage.redModeEnabled)
        assertTrue(storage.automaticSensors)
        assertTrue(storage.autoLocationEnabled)
        assertEquals(AppSettingsStorage.DEFAULT_LOCATION_NAME, storage.locationName)
        assertEquals(AppSettingsStorage.DEFAULT_LATITUDE_TEXT, storage.latitude)
        assertEquals(AppSettingsStorage.DEFAULT_LONGITUDE_TEXT, storage.longitude)

        storage.notificationsEnabled = true
        storage.redModeEnabled = true
        storage.automaticSensors = false
        storage.autoLocationEnabled = false
        storage.locationName = "Kosice"
        storage.latitude = " 49,123 "
        storage.longitude = " 20.456 "

        assertTrue(storage.notificationsEnabled)
        assertTrue(storage.redModeEnabled)
        assertFalse(storage.automaticSensors)
        assertFalse(storage.autoLocationEnabled)
        assertEquals("Kosice", storage.locationName)
        assertEquals("49.123", storage.latitude)
        assertEquals("20.456", storage.longitude)
        assertEquals(49.123, storage.manualLatitude()!!, 0.0001)
        assertEquals(20.456, storage.manualLongitude()!!, 0.0001)
    }

    @Test
    fun `app settings reject invalid coordinates and restore default location`() {
        val storage = AppSettingsStorage(context)

        storage.latitude = "91"
        storage.longitude = "-181"

        assertEquals(AppSettingsStorage.DEFAULT_LATITUDE_TEXT, storage.latitude)
        assertEquals(AppSettingsStorage.DEFAULT_LONGITUDE_TEXT, storage.longitude)
        assertNull(AppSettingsStorage.parseLatitude("not-a-number"))
        assertNull(AppSettingsStorage.parseLongitude("181"))

        storage.locationName = "Custom"
        storage.useDefaultLocation()

        assertFalse(storage.autoLocationEnabled)
        assertEquals(AppSettingsStorage.DEFAULT_LOCATION_NAME, storage.locationName)
        assertEquals(AppSettingsStorage.DEFAULT_LATITUDE, storage.manualLatitude()!!, 0.0001)
        assertEquals(AppSettingsStorage.DEFAULT_LONGITUDE, storage.manualLongitude()!!, 0.0001)
    }

    @Test
    fun `session storage saves valid auth and clears invalid auth`() {
        val storage = SessionStorage(context)
        val token = jwt("AUTH", expiresInSeconds = 3600)

        storage.saveAuth(token, "OBSERVER", "Alice", 42L)

        assertEquals(token, storage.getToken())
        assertEquals(token, storage.peekToken())
        assertEquals("OBSERVER", storage.getRole())
        assertEquals("Alice", storage.getDisplayName())
        assertEquals(42L, storage.getUserId())
        assertEquals(SessionStorage.AUTH_PROVIDER_LOCAL, storage.getAuthProvider())
        assertTrue(storage.isLoggedIn())
        assertFalse(storage.isGoogleAccount())

        storage.updateUserProfile("ADMIN", "Ada", 99L, SessionStorage.AUTH_PROVIDER_GOOGLE)

        assertEquals("ADMIN", storage.getRole())
        assertEquals("Ada", storage.getDisplayName())
        assertEquals(99L, storage.getUserId())
        assertTrue(storage.isGoogleAccount())

        storage.saveAuth(jwt("AUTH", expiresInSeconds = -1), "OBSERVER", "Expired", 1L)

        assertNull(storage.getToken())
        assertFalse(storage.isLoggedIn())
    }

    @Test
    fun `session storage accepts only pending google tokens for pending state`() {
        val storage = SessionStorage(context)
        val pending = jwt("PENDING_GOOGLE", expiresInSeconds = 3600)

        storage.savePendingGoogleToken(pending)
        assertEquals(pending, storage.getPendingGoogleToken())

        storage.savePendingGoogleToken(jwt("AUTH", expiresInSeconds = 3600))
        assertNull(storage.getPendingGoogleToken())

        storage.savePendingGoogleToken(jwt("PENDING_GOOGLE", expiresInSeconds = -1))
        assertNull(storage.getPendingGoogleToken())
    }

    @Test
    fun `profile image cache saves reads and deletes stable files`() {
        val cache = ProfileImageCache(context)
        val bytes = byteArrayOf(1, 2, 3, 4)

        assertNull(cache.getProfilePicture(12L))

        val saved = cache.saveProfilePicture(12L, bytes)

        assertTrue(saved.exists())
        assertArrayEquals(bytes, cache.getProfilePicture(12L)!!.readBytes())

        cache.deleteProfilePicture(12L)
        assertNull(cache.getProfilePicture(12L))

        val fallback = cache.saveProfilePicture(-5L, byteArrayOf(9))
        assertEquals("profile_picture_0", fallback.name)
    }

    private fun jwt(tokenType: String, expiresInSeconds: Long): String {
        val exp = System.currentTimeMillis() / 1000L + expiresInSeconds
        val payload = """{"tokenType":"$tokenType","exp":$exp}"""
        val encodedPayload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray())
        return "header.$encodedPayload.signature"
    }
}
