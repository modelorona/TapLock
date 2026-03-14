package com.ah.taplock

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ah.taplock.ui.theme.TapLockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val sharedPrefName: String
        get() = context.getString(R.string.shared_pref_name)

    private fun getPrefs(): SharedPreferences =
        context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)

    @Before
    fun setup() {
        getPrefs().edit().clear().commit()

        composeTestRule.setContent {
            TapLockTheme {
                TapLockScreen()
            }
        }
    }

    @Test
    fun defaultValues_vibrateOnLockIsOn() {
        composeTestRule.onNodeWithTag("switch_vibrate")
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun defaultValues_showWidgetIconIsOff() {
        composeTestRule.onNodeWithTag("switch_show_icon")
            .performScrollTo()
            .assertIsOff()
    }

    @Test
    fun defaultValues_statusBarDoubleTapIsOff() {
        composeTestRule.onNodeWithTag("switch_status_bar")
            .performScrollTo()
            .assertIsOff()
    }

    @Test
    fun toggleVibrateOff_updatesPref() {
        composeTestRule.onNodeWithTag("switch_vibrate")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_vibrate")
            .assertIsOff()

        assertFalse(getPrefs().getBoolean(context.getString(R.string.vibrate_on_lock), true))
    }

    @Test
    fun toggleVibrateOffThenOn_updatesPref() {
        composeTestRule.onNodeWithTag("switch_vibrate")
            .performScrollTo()
            .performClick() // off
            .performClick() // on

        composeTestRule.onNodeWithTag("switch_vibrate")
            .assertIsOn()

        assertTrue(getPrefs().getBoolean(context.getString(R.string.vibrate_on_lock), false))
    }

    @Test
    fun toggleShowIcon_updatesPref() {
        composeTestRule.onNodeWithTag("switch_show_icon")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_show_icon")
            .assertIsOn()

        assertTrue(getPrefs().getBoolean(context.getString(R.string.show_widget_icon), false))
    }

    @Test
    fun toggleShowIconOn_showsIconButtons() {
        composeTestRule.onNodeWithTag("switch_show_icon")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Select Icon")
            .performScrollTo()
            .assertExists()
        composeTestRule.onNodeWithText("Reset Default")
            .assertExists()
    }

    @Test
    fun toggleStatusBarDoubleTap_updatesPref() {
        composeTestRule.onNodeWithTag("switch_status_bar")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_status_bar")
            .assertIsOn()

        assertTrue(getPrefs().getBoolean(context.getString(R.string.status_bar_double_tap), false))
    }

    @Test
    fun updateTimeout_updatesPref() {
        composeTestRule.onNode(hasSetTextAction())
            .performScrollTo()
            .performTextReplacement("500")

        composeTestRule.onNodeWithText("Update")
            .performScrollTo()
            .performClick()

        assertEquals(500, getPrefs().getInt(context.getString(R.string.double_tap_timeout), 0))
    }
}
