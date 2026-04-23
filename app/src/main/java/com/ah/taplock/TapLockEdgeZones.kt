package com.ah.taplock

import kotlin.math.roundToInt

enum class EdgeZoneSide {
    LEFT,
    RIGHT
}

enum class ZoneOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class CornerZonePosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

data class EdgeZoneFrame(
    val widthPx: Int,
    val heightPx: Int,
    val x: Int,
    val y: Int
)

object TapLockEdgeZones {
    const val DEFAULT_WIDTH_DP = 18
    const val MIN_WIDTH_DP = 12
    const val MAX_WIDTH_DP = 96

    const val DEFAULT_CORNER_SIZE_DP = 48
    const val MIN_CORNER_SIZE_DP = 24
    const val MAX_CORNER_SIZE_DP = 120

    const val DEFAULT_COVERAGE_PERCENT = 45
    const val MIN_COVERAGE_PERCENT = 20
    const val MAX_COVERAGE_PERCENT = 90

    const val DEFAULT_TOP_OFFSET_PERCENT = 28
    const val DEFAULT_BOTTOM_OFFSET_PERCENT = 27
    const val MIN_OFFSET_PERCENT = 0
    const val MAX_OFFSET_PERCENT = 40

    fun isPortrait(screenWidthPx: Int, screenHeightPx: Int): Boolean =
        screenHeightPx >= screenWidthPx

    fun orientationForBounds(screenWidthPx: Int, screenHeightPx: Int): ZoneOrientation =
        if (isPortrait(screenWidthPx, screenHeightPx)) {
            ZoneOrientation.PORTRAIT
        } else {
            ZoneOrientation.LANDSCAPE
        }

    fun prefKey(baseKey: String, orientation: ZoneOrientation): String =
        if (orientation == ZoneOrientation.PORTRAIT) {
            baseKey
        } else {
            "${baseKey}_landscape"
        }

    fun buildFrame(
        screenWidthPx: Int,
        screenHeightPx: Int,
        widthDp: Int,
        density: Float,
        topOffsetPercent: Int,
        bottomOffsetPercent: Int,
        side: EdgeZoneSide
    ): EdgeZoneFrame {
        val clampedWidthDp = widthDp.coerceIn(MIN_WIDTH_DP, MAX_WIDTH_DP)
        val clampedTopOffsetPercent =
            topOffsetPercent.coerceIn(MIN_OFFSET_PERCENT, MAX_OFFSET_PERCENT)
        val clampedBottomOffsetPercent =
            bottomOffsetPercent.coerceIn(MIN_OFFSET_PERCENT, MAX_OFFSET_PERCENT)

        val widthPx = (clampedWidthDp * density).roundToInt().coerceAtLeast(1)
        val topOffsetPx = ((screenHeightPx * clampedTopOffsetPercent) / 100f).roundToInt()
        val bottomOffsetPx =
            ((screenHeightPx * clampedBottomOffsetPercent) / 100f).roundToInt()
        val heightPx = (screenHeightPx - topOffsetPx - bottomOffsetPx).coerceAtLeast(1)
        val y = topOffsetPx.coerceAtLeast(0)
        val x = when (side) {
            EdgeZoneSide.LEFT -> 0
            EdgeZoneSide.RIGHT -> (screenWidthPx - widthPx).coerceAtLeast(0)
        }

        return EdgeZoneFrame(
            widthPx = widthPx,
            heightPx = heightPx,
            x = x,
            y = y
        )
    }

    fun buildCornerFrame(
        screenWidthPx: Int,
        screenHeightPx: Int,
        sizeDp: Int,
        density: Float,
        position: CornerZonePosition
    ): EdgeZoneFrame {
        val clampedSizeDp = sizeDp.coerceIn(MIN_CORNER_SIZE_DP, MAX_CORNER_SIZE_DP)
        val sizePx = (clampedSizeDp * density).roundToInt().coerceAtLeast(1)
        val x = when (position) {
            CornerZonePosition.TOP_LEFT,
            CornerZonePosition.BOTTOM_LEFT -> 0

            CornerZonePosition.TOP_RIGHT,
            CornerZonePosition.BOTTOM_RIGHT -> (screenWidthPx - sizePx).coerceAtLeast(0)
        }
        val y = when (position) {
            CornerZonePosition.TOP_LEFT,
            CornerZonePosition.TOP_RIGHT -> 0

            CornerZonePosition.BOTTOM_LEFT,
            CornerZonePosition.BOTTOM_RIGHT -> (screenHeightPx - sizePx).coerceAtLeast(0)
        }

        return EdgeZoneFrame(
            widthPx = sizePx,
            heightPx = sizePx,
            x = x,
            y = y
        )
    }

    fun deriveOffsetsFromCoverage(coveragePercent: Int): Pair<Int, Int> {
        val clampedCoveragePercent =
            coveragePercent.coerceIn(MIN_COVERAGE_PERCENT, MAX_COVERAGE_PERCENT)
        val remainingPercent = (100 - clampedCoveragePercent).coerceAtLeast(0)
        val topOffsetPercent = remainingPercent / 2
        val bottomOffsetPercent = remainingPercent - topOffsetPercent
        return topOffsetPercent to bottomOffsetPercent
    }
}
