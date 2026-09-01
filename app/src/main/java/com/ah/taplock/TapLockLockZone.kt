package com.ah.taplock

import kotlin.math.roundToInt

/** Resolved geometry for the lock-screen tap zone overlay: [heightPx] tall, starting at [yPx]. */
data class LockZoneFrame(
    val heightPx: Int,
    val yPx: Int
)

/** Pure geometry for the lock-screen tap zone, expressed in percentages of screen height. */
object TapLockLockZone {
    const val DEFAULT_PERCENT = 66
    const val MIN_PERCENT = 20
    const val MAX_PERCENT = 100

    const val DEFAULT_TOP_OFFSET_PERCENT = 0
    const val MIN_TOP_OFFSET_PERCENT = 0

    /** Clamps a zone height [value] into the supported percent range. */
    fun clampPercent(value: Int): Int = value.coerceIn(MIN_PERCENT, MAX_PERCENT)

    /** Largest top offset that still keeps a zone of [zonePercent] fully on screen. */
    fun maxTopOffsetPercent(zonePercent: Int): Int = (100 - clampPercent(zonePercent)).coerceAtLeast(0)

    /** Clamps a top offset [value] so the zone of [zonePercent] never extends past the bottom. */
    fun clampTopOffsetPercent(
        value: Int,
        zonePercent: Int
    ): Int = value.coerceIn(MIN_TOP_OFFSET_PERCENT, maxTopOffsetPercent(zonePercent))

    /**
     * Converts [zonePercent] and [topOffsetPercent] into pixel geometry for a screen of
     * [screenHeightPx]. Inputs are clamped so the result is always a valid on-screen frame.
     */
    fun buildFrame(
        screenHeightPx: Int,
        zonePercent: Int,
        topOffsetPercent: Int
    ): LockZoneFrame {
        val boundedScreenHeightPx = screenHeightPx.coerceAtLeast(1)
        val clampedZonePercent = clampPercent(zonePercent)
        val clampedTopOffsetPercent = clampTopOffsetPercent(topOffsetPercent, clampedZonePercent)
        val heightPx = ((boundedScreenHeightPx * clampedZonePercent) / 100f)
            .roundToInt()
            .coerceIn(1, boundedScreenHeightPx)
        val yPx = ((boundedScreenHeightPx * clampedTopOffsetPercent) / 100f)
            .roundToInt()
            .coerceIn(0, (boundedScreenHeightPx - heightPx).coerceAtLeast(0))

        return LockZoneFrame(
            heightPx = heightPx,
            yPx = yPx
        )
    }
}
