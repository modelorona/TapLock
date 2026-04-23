package com.ah.taplock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapLockEdgeZonesTest {

    @Test
    fun buildFrame_leftEdgeAnchorsToLeftAndUsesTopAndBottomOffsets() {
        val frame = TapLockEdgeZones.buildFrame(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            widthDp = 18,
            density = 2f,
            topOffsetPercent = 10,
            bottomOffsetPercent = 20,
            side = EdgeZoneSide.LEFT
        )

        assertEquals(36, frame.widthPx)
        assertEquals(1680, frame.heightPx)
        assertEquals(0, frame.x)
        assertEquals(240, frame.y)
    }

    @Test
    fun buildFrame_rightEdgeAnchorsToRight() {
        val frame = TapLockEdgeZones.buildFrame(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            widthDp = 24,
            density = 2f,
            topOffsetPercent = 28,
            bottomOffsetPercent = 27,
            side = EdgeZoneSide.RIGHT
        )

        assertEquals(48, frame.widthPx)
        assertEquals(1080, frame.heightPx)
        assertEquals(1032, frame.x)
        assertEquals(672, frame.y)
    }

    @Test
    fun buildFrame_clampsWidthAndOffsetsToAllowedRange() {
        val frame = TapLockEdgeZones.buildFrame(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            widthDp = 99,
            density = 3f,
            topOffsetPercent = 99,
            bottomOffsetPercent = 99,
            side = EdgeZoneSide.LEFT
        )

        assertEquals(TapLockEdgeZones.MAX_WIDTH_DP * 3, frame.widthPx)
        assertEquals(480, frame.heightPx)
        assertEquals(0, frame.x)
        assertEquals(960, frame.y)
    }

    @Test
    fun deriveOffsetsFromCoverage_preservesCenteredLegacyBand() {
        val (topOffsetPercent, bottomOffsetPercent) =
            TapLockEdgeZones.deriveOffsetsFromCoverage(45)

        assertEquals(27, topOffsetPercent)
        assertEquals(28, bottomOffsetPercent)
    }

    @Test
    fun isPortrait_checksDisplayBounds() {
        assertTrue(TapLockEdgeZones.isPortrait(1080, 2400))
        assertTrue(TapLockEdgeZones.isPortrait(1200, 1200))
        assertFalse(TapLockEdgeZones.isPortrait(2400, 1080))
    }
}
