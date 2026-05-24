package com.codepalace.accelerometer.ui.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppSettingsStorage
import com.codepalace.accelerometer.data.local.SessionStorage
import com.codepalace.accelerometer.ui.view.HalfCompassView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActivitySmokeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteSharedPreferences("auth_prefs")
        context.deleteSharedPreferences("app_settings")
        context.deleteDatabase("celestial_database")
    }

    @Test
    fun `auth activity opens login signup and finishes from back`() {
        val controller = Robolectric.buildActivity(AuthActivity::class.java).setup()
        val activity = controller.get()

        activity.findViewById<Button>(R.id.btnGoLogin).performClick()
        assertEquals(LoginActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)

        activity.findViewById<Button>(R.id.btnGoSignup).performClick()
        assertEquals(SignupActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)

        activity.findViewById<View>(R.id.btnBack).performClick()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `login activity validates empty fields and opens signup`() {
        val activity = Robolectric.buildActivity(LoginActivity::class.java).setup().get()
        val email = activity.findViewById<EditText>(R.id.etLoginUsername)
        val password = activity.findViewById<EditText>(R.id.etLoginPassword)

        activity.findViewById<Button>(R.id.btnLogin).performClick()
        assertEquals("Email is required", email.error.toString())

        email.setText("user@example.com")
        activity.findViewById<Button>(R.id.btnLogin).performClick()
        assertEquals("Password is required", password.error.toString())

        activity.findViewById<View>(R.id.tvGoSignupFromLogin).performClick()
        assertEquals(SignupActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
    }

    @Test
    fun `signup activity validates form fields and opens login`() {
        val activity = Robolectric.buildActivity(SignupActivity::class.java).setup().get()
        val name = activity.findViewById<EditText>(R.id.etSignupUsername)
        val email = activity.findViewById<EditText>(R.id.etSignupEmail)
        val password = activity.findViewById<EditText>(R.id.etSignupPassword)
        val repeatPassword = activity.findViewById<EditText>(R.id.etSignupRepeatPassword)

        activity.findViewById<Button>(R.id.btnSignup).performClick()
        assertEquals("Minimum 3 characters", name.error.toString())

        name.setText("Alice")
        activity.findViewById<Button>(R.id.btnSignup).performClick()
        assertEquals("Email is required", email.error.toString())

        email.setText("alice@example.com")
        activity.findViewById<Button>(R.id.btnSignup).performClick()
        assertEquals("Minimum 8 characters", password.error.toString())

        password.setText("12345678")
        repeatPassword.setText("different")
        activity.findViewById<Button>(R.id.btnSignup).performClick()
        assertEquals("Passwords do not match", repeatPassword.error.toString())

        activity.findViewById<View>(R.id.tvGoLoginFromSignup).performClick()
        assertEquals(LoginActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
    }

    @Test
    fun `settings activity toggles settings and opens account auth when logged out`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val storage = AppSettingsStorage(activity)

        activity.findViewById<SwitchCompat>(R.id.switchNotifications).isChecked = true
        activity.findViewById<SwitchCompat>(R.id.switchRedMode).isChecked = true
        activity.findViewById<SwitchCompat>(R.id.switchSensors).isChecked = false

        assertTrue(storage.notificationsEnabled)
        assertTrue(storage.redModeEnabled)
        assertFalse(storage.automaticSensors)

        activity.findViewById<LinearLayout>(R.id.itemAccount).performClick()
        assertEquals(AuthActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)

        activity.findViewById<LinearLayout>(R.id.itemLocation).performClick()
        assertEquals(LocationSettingsActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
    }

    @Test
    fun `location settings validates manual coordinates and saves valid state`() {
        val activity = Robolectric.buildActivity(LocationSettingsActivity::class.java).setup().get()
        val storage = AppSettingsStorage(activity)
        val auto = activity.findViewById<SwitchCompat>(R.id.switchAutoLocation)
        val latitude = activity.findViewById<EditText>(R.id.etLatitude)
        val longitude = activity.findViewById<EditText>(R.id.etLongitude)

        auto.isChecked = false
        assertTrue(latitude.isEnabled)
        assertTrue(longitude.isEnabled)

        latitude.setText("91")
        longitude.setText("17")
        activity.findViewById<Button>(R.id.btnSaveLocation).performClick()
        assertEquals("Latitude must be from -90 to 90", latitude.error.toString())

        latitude.setText("48.1")
        longitude.setText("181")
        activity.findViewById<Button>(R.id.btnSaveLocation).performClick()
        assertEquals("Longitude must be from -180 to 180", longitude.error.toString())

        latitude.setText("48.1")
        longitude.setText("17.2")
        activity.findViewById<Button>(R.id.btnSaveLocation).performClick()

        assertFalse(storage.autoLocationEnabled)
        assertEquals("48.1", storage.latitude)
        assertEquals("17.2", storage.longitude)

        auto.isChecked = true
        assertFalse(latitude.isEnabled)
        activity.findViewById<Button>(R.id.btnSaveLocation).performClick()
        assertTrue(storage.autoLocationEnabled)
    }

    @Test
    fun `create event activity blocks invalid data before submitting`() {
        ApiClient.init(context)
        ApiClient.getSessionStorage().saveAuth(jwt("AUTH", 3600), "CONTRIBUTOR", "Alice", 1L)
        val activity = Robolectric.buildActivity(CreateEventActivity::class.java).setup().get()
        val eventName = activity.findViewById<EditText>(R.id.etEventName)
        val description = activity.findViewById<EditText>(R.id.etDescription)
        val saveButton = activity.findViewById<Button>(R.id.btnSaveEvent)

        saveButton.performClick()

        assertEquals("Event name is required", eventName.error.toString())

        eventName.setText("Meteor watch")
        saveButton.performClick()

        assertEquals("Description is required", description.error.toString())
        assertEquals("Save Event", saveButton.text.toString())
    }

    @Test
    fun `menu activity switches between auth and logout states`() {
        val controller = Robolectric.buildActivity(MenuActivity::class.java).setup()
        val activity = controller.get()

        assertEquals(View.VISIBLE, activity.findViewById<LinearLayout>(R.id.itemAuthentication).visibility)
        assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.itemLogout).visibility)

        activity.findViewById<LinearLayout>(R.id.itemProfile).performClick()
        assertEquals(AuthActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)

        ApiClient.getSessionStorage().saveAuth(jwt("AUTH", 3600), "OBSERVER", "Alice", 1L)
        controller.pause().resume()

        assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.itemAuthentication).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<LinearLayout>(R.id.itemLogout).visibility)
    }

    @Test
    fun `chat activity ignores empty send clicks`() {
        val intent = ChatActivity.createIntent(context, 1L, "General")
        val activity = Robolectric.buildActivity(ChatActivity::class.java, intent).setup().get()
        val input = activity.findViewById<EditText>(R.id.etMessageInput)
        val messages = activity.findViewById<RecyclerView>(R.id.rvMessages)

        input.setText("   ")
        activity.findViewById<ImageButton>(R.id.btnSend).performClick()

        assertEquals("   ", input.text.toString())
        assertEquals(0, messages.adapter?.itemCount)
    }

    @Test
    fun `half compass view draws headings without crashing`() {
        val view = HalfCompassView(context)
        val bitmap = Bitmap.createBitmap(300, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        view.layout(0, 0, 300, 160)
        view.headingDeg = 45f
        view.draw(canvas)
        view.headingDeg = -30f
        view.draw(canvas)

        assertEquals(300, bitmap.width)
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
