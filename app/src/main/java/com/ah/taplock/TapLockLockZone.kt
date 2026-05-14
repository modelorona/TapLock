package com.ah.taplock

import kotlin.math.roundToInt

data class LockZoneFrame(
    val heightPx: Int,
    val yPx: Int
)

object TapLockLockZone {
    const val DEFAULT_PERCENT = 66
    const val MIN_PERCENT = 20
    const val MAX_PERCENT = 100

    const val DEFAULT_TOP_OFFSET_PERCENT = 0
    const val MIN_TOP_OFFSET_PERCENT = 0

    fun clampPercent(value: Int): Int = value.coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun maxTopOffsetPercent(zonePercent: Int): Int = (100 - clampPercent(zonePercent)).coerceAtLeast(0)

    fun clampTopOffsetPercent(
        value: Int,
        zonePercent: Int
    ): Int = value.coerceIn(MIN_TOP_OFFSET_PERCENT, maxTopOffsetPercent(zonePercent))

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
