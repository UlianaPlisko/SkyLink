package com.codepalace.accelerometer.ui.activity

import android.content.Context
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppSettingsStorage
import com.codepalace.accelerometer.testing.AndroidTestSupport
import com.codepalace.accelerometer.testing.MockBackend
import com.codepalace.accelerometer.testing.TestTokens
import com.codepalace.accelerometer.util.EventFormValidator
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class UatEspressoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockBackend = MockBackend(context)

    @After
    fun tearDown() {
        mockBackend.stop()
        AndroidTestSupport.clearAppState(context)
    }

    @Test
    fun uat0LoginScreenPreventsSubmittingEmptyCredentials() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.tvLoginTitle)).check(matches(withText("Welcome Back!")))

            onView(withId(R.id.btnLogin)).perform(click())

            onView(withId(R.id.etLoginUsername))
                .check(matches(hasErrorText("Email is required")))
        }
    }

    @Test
    fun uat0LoginWithValidCredentialsUsesMockedServerResponse() {
        mockBackend.start()

        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.etLoginUsername))
                .perform(replaceText("mock.user@example.com"), closeSoftKeyboard())
            onView(withId(R.id.etLoginPassword))
                .perform(replaceText("Password123"), closeSoftKeyboard())

            onView(withId(R.id.btnLogin)).perform(click())

            AndroidTestSupport.waitUntilDisplayed(R.id.skyView)
            onView(withId(R.id.btnSearch)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun uat4CreateEventScreenShowsValidationForMissingData() {
        saveContributorSession()

        ActivityScenario.launch(CreateEventActivity::class.java).use {
            onView(withId(R.id.etEventName)).check(matches(isDisplayed()))

            onView(withId(R.id.btnSaveEvent)).perform(scrollTo(), click())

            onView(withId(R.id.etEventName))
                .check(matches(hasErrorText(EventFormValidator.EVENT_NAME_REQUIRED)))

            onView(withId(R.id.etEventName))
                .perform(scrollTo(), replaceText("Meteor watch"), closeSoftKeyboard())
            onView(withId(R.id.btnSaveEvent)).perform(scrollTo(), click())

            onView(withId(R.id.etDescription))
                .check(matches(hasErrorText(EventFormValidator.DESCRIPTION_REQUIRED)))
        }
    }

    @Test
    fun uat5LocationScreenEnablesAutoLocation() {
        AppSettingsStorage(context).autoLocationEnabled = false

        ActivityScenario.launch(LocationSettingsActivity::class.java).use {
            onView(withId(R.id.etLatitude)).check(matches(isEnabled()))

            onView(withId(R.id.switchAutoLocation)).perform(AndroidTestSupport.setChecked(true))

            onView(withId(R.id.switchAutoLocation)).check(matches(isChecked()))
            onView(withId(R.id.etLatitude)).check(matches(not(isEnabled())))
            onView(withId(R.id.etLongitude)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun e2eSettingsToLocationAutoLocationFlow() {
        AppSettingsStorage(context).autoLocationEnabled = false

        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText("Settings")).check(matches(isDisplayed()))

            onView(withId(R.id.itemLocation)).perform(click())
            onView(withText("Location Settings")).check(matches(isDisplayed()))

            onView(withId(R.id.etLatitude))
                .perform(replaceText("48.1486"), closeSoftKeyboard())
            onView(withId(R.id.etLongitude))
                .perform(replaceText("17.1077"), closeSoftKeyboard())
            onView(withId(R.id.switchAutoLocation)).perform(AndroidTestSupport.setChecked(true))
            onView(withId(R.id.btnSaveLocation)).perform(click())

            onView(withId(R.id.switchAutoLocation)).check(matches(isChecked()))
            onView(withId(R.id.etLatitude)).check(matches(not(isEnabled())))
        }
    }

    private fun saveContributorSession() {
        ApiClient.init(context)
        ApiClient.getSessionStorage().saveAuth(
            token = TestTokens.authJwt(),
            role = "CONTRIBUTOR",
            displayName = "UAT Contributor",
            userId = 7L
        )
    }
}
