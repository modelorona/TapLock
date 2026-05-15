package com.ah.taplock

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class TapLockBatteryOptimizationTest {

    @Test
    fun packageUriString_targetsThisPackage() {
        assertEquals(
            "package:com.ah.taplock",
            TapLockBatteryOptimization.packageUriString("com.ah.taplock")
        )
    }

    @Test
    fun actions_matchAndroidConstants() {
        assertEquals(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            TapLockBatteryOptimization.REQUEST_ACTION
        )
        assertEquals(
            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            TapLockBatteryOptimization.SETTINGS_ACTION
        )
    }
}
