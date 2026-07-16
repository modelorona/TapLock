package com.ah.taplock

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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

    private fun setScreenContent() {
        composeTestRule.setContent {
            TapLockTheme {
                TapLockScreen()
            }
        }
    }

    @Before
    fun setup() {
        getPrefs().edit().clear().commit()
        setScreenContent()
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
    fun defaultValues_statusBarModeIsOff() {
        composeTestRule.onNodeWithTag("status_bar_mode_off")
            .performScrollTo()
            .assertIsSelected()
    }

    @Test
    fun defaultValues_interactionZonesAreOff() {
        composeTestRule.onNodeWithTag("left_edge_mode_off")
            .performScrollTo()
            .assertIsSelected()

        composeTestRule.onNodeWithTag("right_edge_mode_off")
            .performScrollTo()
            .assertIsSelected()

        composeTestRule.onNodeWithTag("top_left_corner_mode_off")
            .performScrollTo()
            .assertIsSelected()

        composeTestRule.onNodeWithTag("top_right_corner_mode_off")
            .performScrollTo()
            .assertIsSelected()

        composeTestRule.onNodeWithTag("bottom_left_corner_mode_off")
            .performScrollTo()
            .assertIsSelected()

        composeTestRule.onNodeWithTag("bottom_right_corner_mode_off")
            .performScrollTo()
            .assertIsSelected()
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
    fun setStatusBarModeToDoubleTap_updatesPref() {
        composeTestRule.onNodeWithTag("status_bar_mode_double_tap")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("status_bar_mode_double_tap")
            .assertIsSelected()

        assertEquals(
            TapZoneMode.DOUBLE_TAP.name,
            getPrefs().getString(context.getString(R.string.status_bar_mode), null)
        )
    }

    @Test
    fun toggleStatusBarCameraAreaOnly_updatesPref() {
        getPrefs().edit()
            .putString(context.getString(R.string.status_bar_mode), TapZoneMode.DOUBLE_TAP.name)
            .commit()

        setScreenContent()

        composeTestRule.onNodeWithTag("switch_status_bar_camera_area")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_status_bar_camera_area")
            .assertIsOn()

        assertTrue(
            getPrefs().getBoolean(context.getString(R.string.status_bar_camera_area_only), false)
        )
    }

    @Test
    fun setLeftEdgeModeToDoubleTap_updatesPref() {
        composeTestRule.onNodeWithTag("left_edge_mode_double_tap")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("left_edge_mode_double_tap")
            .assertIsSelected()

        assertEquals(
            TapZoneMode.DOUBLE_TAP.name,
            getPrefs().getString(context.getString(R.string.left_edge_mode), null)
        )
    }

    @Test
    fun setRightEdgeModeToDoubleTap_updatesPref() {
        composeTestRule.onNodeWithTag("right_edge_mode_double_tap")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("right_edge_mode_double_tap")
            .assertIsSelected()

        assertEquals(
            TapZoneMode.DOUBLE_TAP.name,
            getPrefs().getString(context.getString(R.string.right_edge_mode), null)
        )
    }

    @Test
    fun setTopLeftCornerModeToDoubleTap_updatesPref() {
        composeTestRule.onNodeWithTag("top_left_corner_mode_double_tap")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("top_left_corner_mode_double_tap")
            .assertIsSelected()

        assertEquals(
            TapZoneMode.DOUBLE_TAP.name,
            getPrefs().getString(context.getString(R.string.top_left_corner_mode), null)
        )
    }

    @Test
    fun updateTopEdgeOffset_updatesPref() {
        composeTestRule.onNodeWithTag("left_edge_mode_double_tap")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("slider_edge_zone_top_offset")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(12f))
            }

        assertEquals(
            12,
            getPrefs().getInt(
                context.getString(R.string.edge_zone_top_offset_percent),
                TapLockEdgeZones.DEFAULT_TOP_OFFSET_PERCENT
            )
        )
    }

    @Test
    fun updateBottomEdgeOffset_updatesPref() {
        composeTestRule.onNodeWithTag("left_edge_mode_double_tap")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("slider_edge_zone_bottom_offset")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(15f))
            }

        assertEquals(
            15,
            getPrefs().getInt(
                context.getString(R.string.edge_zone_bottom_offset_percent),
                TapLockEdgeZones.DEFAULT_BOTTOM_OFFSET_PERCENT
            )
        )
    }

    @Test
    fun updateCornerZoneSize_updatesPref() {
        composeTestRule.onNodeWithTag("top_left_corner_mode_double_tap")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("slider_corner_zone_size")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(72f))
            }

        assertEquals(
            72,
            getPrefs().getInt(
                context.getString(R.string.corner_zone_size_dp),
                TapLockEdgeZones.DEFAULT_CORNER_SIZE_DP
            )
        )
    }

    @Test
    fun updateLockZone_updatesPref() {
        getPrefs().edit()
            .putString(context.getString(R.string.lock_screen_mode), TapZoneMode.DOUBLE_TAP.name)
            .commit()

        setScreenContent()

        composeTestRule.onNodeWithTag("slider_lock_zone")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(80f))
            }

        assertEquals(
            80,
            getPrefs().getInt(context.getString(R.string.lock_zone_percent), 0)
        )
    }

    @Test
    fun updateLockZoneTopOffset_updatesPref() {
        getPrefs().edit()
            .putString(context.getString(R.string.lock_screen_mode), TapZoneMode.DOUBLE_TAP.name)
            .commit()

        setScreenContent()

        composeTestRule.onNodeWithTag("slider_lock_zone_top_offset")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(12f))
            }

        assertEquals(
            12,
            getPrefs().getInt(context.getString(R.string.lock_zone_top_offset_percent), -1)
        )
    }

    @Test
    fun highlightActiveArea_showsOverlayPreview() {
        getPrefs().edit()
            .putString(context.getString(R.string.lock_screen_mode), TapZoneMode.DOUBLE_TAP.name)
            .commit()

        setScreenContent()

        composeTestRule.onNodeWithTag("button_lock_zone_preview")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("lock_zone_live_overlay")
            .assertExists()
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

    @Test
    fun selectWidgetStyle_updatesPref() {
        composeTestRule.onNodeWithTag("chip_widget_style_glass")
            .performScrollTo()
            .performClick()

        assertEquals(
            TapLockWidgetStyle.GLASS.name,
            getPrefs().getString(context.getString(R.string.widget_style), null)
        )
    }

    @Test
    fun floatingButtonSliders_updatePrefs() {
        getPrefs().edit()
            .putBoolean(context.getString(R.string.floating_button_enabled), true)
            .putInt(
                context.getString(R.string.floating_button_size_dp),
                TapLockFloatingButtonConfig.DEFAULT_SIZE_DP
            )
            .putInt(
                context.getString(R.string.floating_button_opacity_percent),
                TapLockFloatingButtonConfig.DEFAULT_OPACITY_PERCENT
            )
            .commit()

        setScreenContent()

        composeTestRule.onNodeWithTag("slider_floating_button_size")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(72f))
            }

        composeTestRule.onNodeWithTag("slider_floating_button_opacity")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(65f))
            }

        assertEquals(
            72,
            getPrefs().getInt(
                context.getString(R.string.floating_button_size_dp),
                TapLockFloatingButtonConfig.DEFAULT_SIZE_DP
            )
        )
        assertEquals(
            65,
            getPrefs().getInt(
                context.getString(R.string.floating_button_opacity_percent),
                TapLockFloatingButtonConfig.DEFAULT_OPACITY_PERCENT
            )
        )
    }
}
