package com.ah.taplock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DoubleTapDetectorTest {

    private var fakeTime = 1000L
    private lateinit var detector: DoubleTapDetector

    @Before
    fun setup() {
        fakeTime = 1000L
        detector = DoubleTapDetector(currentTimeMs = { fakeTime })
    }

    @Test
    fun firstTap_returnsNoDoubleTap() {
        assertFalse(detector.onTap(300))
    }

    @Test
    fun twoTapsWithinTimeout_returnsDoubleTap() {
        assertFalse(detector.onTap(300))
        fakeTime += 200
        assertTrue(detector.onTap(300))
    }

    @Test
    fun twoTapsExceedingTimeout_returnsNoDoubleTap() {
        assertFalse(detector.onTap(300))
        fakeTime += 400
        assertFalse(detector.onTap(300))
    }

    @Test
    fun twoTapsAtExactTimeout_returnsNoDoubleTap() {
        assertFalse(detector.onTap(300))
        fakeTime += 300
        assertFalse(detector.onTap(300))
    }

    @Test
    fun twoTapsJustUnderTimeout_returnsDoubleTap() {
        assertFalse(detector.onTap(300))
        fakeTime += 299
        assertTrue(detector.onTap(300))
    }

    @Test
    fun tripleTap_onlySecondTriggers() {
        assertFalse(detector.onTap(300))
        fakeTime += 100
        assertTrue(detector.onTap(300))
        fakeTime += 100
        assertFalse(detector.onTap(300))
    }

    @Test
    fun afterDoubleTap_nextPairCanDoubleTapAgain() {
        assertFalse(detector.onTap(300))
        fakeTime += 100
        assertTrue(detector.onTap(300))

        fakeTime += 50
        assertFalse(detector.onTap(300))
        fakeTime += 100
        assertTrue(detector.onTap(300))
    }

    @Test
    fun reset_clearsState() {
        assertFalse(detector.onTap(300))
        fakeTime += 100
        detector.reset()
        assertFalse(detector.onTap(300))
    }

    @Test
    fun differentTimeouts_respected() {
        assertFalse(detector.onTap(100))
        fakeTime += 150
        assertFalse(detector.onTap(100)) // 150ms > 100ms timeout

        fakeTime += 501 // exceed any reasonable timeout so next tap is fresh
        assertFalse(detector.onTap(500)) // fresh first tap
        fakeTime += 400
        assertTrue(detector.onTap(500))  // 400ms < 500ms timeout
    }

    @Test
    fun zeroTimeout_neverDoubleTaps() {
        assertFalse(detector.onTap(0))
        assertFalse(detector.onTap(0))
        assertFalse(detector.onTap(0))
    }

    @Test
    fun simultaneousTaps_countsAsDoubleTap() {
        assertFalse(detector.onTap(300))
        // fakeTime unchanged — 0ms elapsed
        assertTrue(detector.onTap(300))
    }
}
