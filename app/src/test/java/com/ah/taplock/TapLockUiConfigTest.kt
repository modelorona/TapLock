package com.ah.taplock

import org.junit.Assert.assertEquals
import org.junit.Test

class TapLockUiConfigTest {

    @Test
    fun widgetStyle_fromStoredFallsBackToDefault() {
        assertEquals(
            TapLockWidgetStyle.default,
            TapLockWidgetStyle.fromStored("not_a_real_style")
        )
    }

    @Test
    fun floatingButtonConfig_clampsRange() {
        assertEquals(
            TapLockFloatingButtonConfig.MIN_SIZE_DP,
            TapLockFloatingButtonConfig.clampSizeDp(1)
        )
        assertEquals(
            TapLockFloatingButtonConfig.MAX_SIZE_DP,
            TapLockFloatingButtonConfig.clampSizeDp(999)
        )
        assertEquals(
            TapLockFloatingButtonConfig.MIN_OPACITY_PERCENT,
            TapLockFloatingButtonConfig.clampOpacityPercent(1)
        )
        assertEquals(
            TapLockFloatingButtonConfig.MAX_OPACITY_PERCENT,
            TapLockFloatingButtonConfig.clampOpacityPercent(999)
        )
    }
}
