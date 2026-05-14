package com.ah.taplock

import org.junit.Assert.assertEquals
import org.junit.Test

class TapLockLockZoneTest {

    @Test
    fun buildFrame_topAnchorsByDefault() {
        val frame = TapLockLockZone.buildFrame(
            screenHeightPx = 2400,
            zonePercent = 66,
            topOffsetPercent = 0
        )

        assertEquals(1584, frame.heightPx)
        assertEquals(0, frame.yPx)
    }

    @Test
    fun buildFrame_appliesTopOffset() {
        val frame = TapLockLockZone.buildFrame(
            screenHeightPx = 2400,
            zonePercent = 50,
            topOffsetPercent = 12
        )

        assertEquals(1200, frame.heightPx)
        assertEquals(288, frame.yPx)
    }

    @Test
    fun buildFrame_clampsOffsetToRemainingSpace() {
        val frame = TapLockLockZone.buildFrame(
            screenHeightPx = 2400,
            zonePercent = 80,
            topOffsetPercent = 60
        )

        assertEquals(1920, frame.heightPx)
        assertEquals(480, frame.yPx)
    }
}
