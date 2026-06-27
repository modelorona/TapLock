package com.ah.taplock

import kotlin.math.roundToInt

data class StatusBarCutoutBounds(
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int
) {
    val isValid: Boolean
        get() = rightPx > leftPx && bottomPx > topPx
}

data class StatusBarZoneFrame(
    val widthPx: Int,
    val xPx: Int
)

object TapLockStatusBarZone {
    const val CAMERA_AREA_FALLBACK_WIDTH_DP = 96
    const val CAMERA_AREA_MIN_WIDTH_DP = 72
    const val CAMERA_AREA_HORIZONTAL_PADDING_DP = 24

    fun buildCameraAreaFrame(
        screenWidthPx: Int,
        statusBarHeightPx: Int,
        cutoutBounds: List<StatusBarCutoutBounds>,
        density: Float
    ): StatusBarZoneFrame {
        val boundedScreenWidthPx = screenWidthPx.coerceAtLeast(1)
        val boundedStatusBarHeightPx = statusBarHeightPx.coerceAtLeast(1)
        val boundedDensity = density.takeIf { it > 0f } ?: 1f
        val fallbackWidthPx = dpToPx(CAMERA_AREA_FALLBACK_WIDTH_DP, boundedDensity)
        val minWidthPx = dpToPx(CAMERA_AREA_MIN_WIDTH_DP, boundedDensity)
        val paddingPx = dpToPx(CAMERA_AREA_HORIZONTAL_PADDING_DP, boundedDensity)

        val topCutouts = cutoutBounds.filter { cutout ->
            cutout.isValid &&
                cutout.topPx <= boundedStatusBarHeightPx &&
                cutout.bottomPx >= 0
        }

        if (topCutouts.isEmpty()) {
            val widthPx = fallbackWidthPx.coerceIn(1, boundedScreenWidthPx)
            return StatusBarZoneFrame(
                widthPx = widthPx,
                xPx = ((boundedScreenWidthPx - widthPx) / 2f).roundToInt()
            )
        }

        val leftPx = topCutouts.minOf { it.leftPx }
        val rightPx = topCutouts.maxOf { it.rightPx }
        val cutoutCenterPx = (leftPx + rightPx) / 2f
        val widthPx = (rightPx - leftPx + paddingPx * 2)
            .coerceAtLeast(minWidthPx)
            .coerceAtMost(boundedScreenWidthPx)
        val maxXPx = (boundedScreenWidthPx - widthPx).coerceAtLeast(0)
        val xPx = (cutoutCenterPx - widthPx / 2f)
            .roundToInt()
            .coerceIn(0, maxXPx)

        return StatusBarZoneFrame(
            widthPx = widthPx,
            xPx = xPx
        )
    }

    private fun dpToPx(valueDp: Int, density: Float): Int =
        (valueDp * density).roundToInt().coerceAtLeast(1)
}
