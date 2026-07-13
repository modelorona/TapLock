package com.ah.taplock

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ah.taplock.ui.theme.TapLockTheme
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * Drives [TapLockScreen] and captures store screenshots via fastlane screengrab.
 * Run through the `fastlane screenshots` lane, not as part of the normal test suite —
 * it produces images rather than assertions. The [LocaleTestRule] switches the device
 * locale for each locale listed in Screengrabfile so descriptions render translated.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setScreenContent() {
        composeTestRule.setContent {
            TapLockTheme {
                TapLockScreen()
            }
        }
    }

    @Before
    fun setup() {
        // Seed a configured state so the widget and floating-button previews render richly:
        // custom-looking icon shown, glass widget style, floating button enabled, and the
        // lock-screen double-tap zone controls revealed. Onboarding/info are dismissed.
        context.getSharedPreferences(
            context.getString(R.string.shared_pref_name),
            Context.MODE_PRIVATE
        ).edit().clear()
            .putBoolean(context.getString(R.string.has_completed_onboarding), true)
            .putBoolean(context.getString(R.string.has_seen_info), true)
            .putBoolean(context.getString(R.string.show_widget_icon), true)
            .putString(context.getString(R.string.widget_style), TapLockWidgetStyle.GLASS.name)
            .putBoolean(context.getString(R.string.floating_button_enabled), true)
            .putInt(context.getString(R.string.floating_button_size_dp), 72)
            .putInt(context.getString(R.string.floating_button_opacity_percent), 90)
            .putBoolean(context.getString(R.string.lock_screen_double_tap), true)
            .commit()
    }

    @Test
    fun captureScreenshots() {
        setScreenContent()

        // 1 — top of the settings screen (permissions + quick access).
        composeTestRule.waitForIdle()
        Screengrab.screenshot("01_settings")

        // 2 — the live widget preview (custom icon + glass style) with the style chips above it.
        composeTestRule.onNodeWithTag("widget_style_preview").performScrollTo()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("02_widget_styles")

        // 3 — Quick Settings tile setup.
        composeTestRule.onNodeWithTag("button_quick_settings_tile").performScrollTo()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("03_quick_tile")

        // 4 — ambient triggers with the lock-screen zone controls revealed.
        composeTestRule.onNodeWithTag("switch_lock_screen").performScrollTo()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("04_ambient_triggers")

        // 5 — behavior section (timeout, vibration, lock delay).
        composeTestRule.onNodeWithTag("slider_timeout").performScrollTo()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("05_behavior")
    }

    companion object {
        @get:ClassRule
        @JvmStatic
        val localeTestRule = LocaleTestRule()
    }
}
