package com.codepalace.accelerometer.ui.activity

import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.codepalace.accelerometer.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AuthFlowEspressoTest {

    @Test
    fun authScreenShowsLoginSignupAndGoogleActions() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.tvAuthTitle)).check(matches(withText("Authentication")))
            onView(withId(R.id.btnGoLogin)).check(matches(isDisplayed()))
            onView(withId(R.id.btnGoSignup)).check(matches(isDisplayed()))
            onView(withId(R.id.btnGoogle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun loginScreenValidatesEmptyEmail() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.tvLoginTitle)).check(matches(withText("Welcome Back!")))
            onView(withId(R.id.btnLogin)).perform(click())
            onView(withId(R.id.etLoginUsername)).check(matches(hasErrorText("Email is required")))
        }
    }

    @Test
    fun signupScreenValidatesShortUsername() {
        ActivityScenario.launch(SignupActivity::class.java).use {
            onView(withId(R.id.tvSignupTitle)).check(matches(withText("Create Account Now!")))
            onView(withId(R.id.btnSignup)).perform(click())
            onView(withId(R.id.etSignupUsername)).check(matches(hasErrorText("Minimum 3 characters")))
        }
    }

    @Test
    fun e2eAuthToSignupValidationFlow() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.btnGoLogin)).perform(click())
            onView(withId(R.id.tvLoginTitle)).check(matches(withText("Welcome Back!")))

            onView(withId(R.id.tvGoSignupFromLogin)).perform(click())
            onView(withId(R.id.tvSignupTitle)).check(matches(withText("Create Account Now!")))

            onView(withId(R.id.etSignupUsername)).perform(replaceText("Sky User"), closeSoftKeyboard())
            onView(withId(R.id.etSignupEmail)).perform(replaceText("sky@example.com"), closeSoftKeyboard())
            onView(withId(R.id.etSignupPassword)).perform(replaceText("password1"), closeSoftKeyboard())
            onView(withId(R.id.etSignupRepeatPassword)).perform(replaceText("different1"), closeSoftKeyboard())
            onView(withId(R.id.rbObserver)).perform(click())
            onView(withId(R.id.btnSignup)).perform(click())

            onView(withId(R.id.etSignupRepeatPassword)).check(matches(hasErrorText("Passwords do not match")))
        }
    }
}
