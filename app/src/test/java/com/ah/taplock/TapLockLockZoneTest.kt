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

    @Test
    fun buildCameraAreaFrame_centersFallbackWithoutCutout() {
        val frame = TapLockStatusBarZone.buildCameraAreaFrame(
            screenWidthPx = 1080,
            statusBarHeightPx = 96,
            cutoutBounds = emptyList(),
            density = 3f
        )

        assertEquals(288, frame.widthPx)
        assertEquals(396, frame.xPx)
    }

    @Test
    fun buildCameraAreaFrame_wrapsReportedTopCutout() {
        val frame = TapLockStatusBarZone.buildCameraAreaFrame(
            screenWidthPx = 1080,
            statusBarHeightPx = 96,
            cutoutBounds = listOf(
                StatusBarCutoutBounds(
                    leftPx = 470,
                    topPx = 0,
                    rightPx = 610,
                    bottomPx = 90
                )
            ),
            density = 3f
        )

        assertEquals(284, frame.widthPx)
        assertEquals(398, frame.xPx)
    }

    @Test
    fun buildCameraAreaFrame_clampsEdgeCameraToScreen() {
        val frame = TapLockStatusBarZone.buildCameraAreaFrame(
            screenWidthPx = 1080,
            statusBarHeightPx = 96,
            cutoutBounds = listOf(
                StatusBarCutoutBounds(
                    leftPx = 0,
                    topPx = 0,
                    rightPx = 80,
                    bottomPx = 90
                )
            ),
            density = 3f
        )

        assertEquals(224, frame.widthPx)
        assertEquals(0, frame.xPx)
    }
}
