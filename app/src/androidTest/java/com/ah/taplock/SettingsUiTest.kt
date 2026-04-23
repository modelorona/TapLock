package com.ah.taplock

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
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

    private fun landscapeKey(baseResId: Int): String =
        TapLockEdgeZones.prefKey(context.getString(baseResId), ZoneOrientation.LANDSCAPE)

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
    fun defaultValues_interactionZonesAreOff() {
        composeTestRule.onNodeWithTag("switch_left_edge_zone")
            .performScrollTo()
            .assertIsOff()

        composeTestRule.onNodeWithTag("switch_right_edge_zone")
            .performScrollTo()
            .assertIsOff()

        composeTestRule.onNodeWithTag("switch_top_left_corner_zone")
            .performScrollTo()
            .assertIsOff()

        composeTestRule.onNodeWithTag("switch_top_right_corner_zone")
            .performScrollTo()
            .assertIsOff()

        composeTestRule.onNodeWithTag("switch_bottom_left_corner_zone")
            .performScrollTo()
            .assertIsOff()

        composeTestRule.onNodeWithTag("switch_bottom_right_corner_zone")
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
    fun toggleLeftEdgeZone_updatesPref() {
        composeTestRule.onNodeWithTag("switch_left_edge_zone")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_left_edge_zone")
            .assertIsOn()

        assertTrue(getPrefs().getBoolean(context.getString(R.string.left_edge_lock_zone), false))
    }

    @Test
    fun toggleRightEdgeZone_updatesPref() {
        composeTestRule.onNodeWithTag("switch_right_edge_zone")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_right_edge_zone")
            .assertIsOn()

        assertTrue(getPrefs().getBoolean(context.getString(R.string.right_edge_lock_zone), false))
    }

    @Test
    fun toggleTopLeftCornerZone_updatesPref() {
        composeTestRule.onNodeWithTag("switch_top_left_corner_zone")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_top_left_corner_zone")
            .assertIsOn()

        assertTrue(
            getPrefs().getBoolean(context.getString(R.string.top_left_corner_lock_zone), false)
        )
    }

    @Test
    fun toggleLandscapeLeftEdgeZone_updatesLandscapePref() {
        composeTestRule.onNodeWithTag("chip_zone_orientation_landscape")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_left_edge_zone")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_left_edge_zone")
            .assertIsOn()

        assertTrue(getPrefs().getBoolean(landscapeKey(R.string.left_edge_lock_zone), false))
        assertFalse(getPrefs().getBoolean(context.getString(R.string.left_edge_lock_zone), false))
    }

    @Test
    fun updateTopEdgeOffset_updatesPref() {
        composeTestRule.onNodeWithTag("switch_left_edge_zone")
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
        composeTestRule.onNodeWithTag("switch_left_edge_zone")
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
        composeTestRule.onNodeWithTag("switch_top_left_corner_zone")
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
    fun updateLandscapeCornerZoneSize_updatesLandscapePref() {
        composeTestRule.onNodeWithTag("chip_zone_orientation_landscape")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("switch_top_left_corner_zone")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("slider_corner_zone_size")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue(setProgress(84f))
            }

        assertEquals(
            84,
            getPrefs().getInt(
                landscapeKey(R.string.corner_zone_size_dp),
                TapLockEdgeZones.DEFAULT_CORNER_SIZE_DP
            )
        )
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
