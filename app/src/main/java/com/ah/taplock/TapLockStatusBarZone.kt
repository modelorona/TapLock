package com.ah.taplock

import kotlin.math.roundToInt

/** Display-cutout rectangle in pixels, as reported by the window insets. */
data class StatusBarCutoutBounds(
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int
) {
    /** Guards against degenerate rects the platform occasionally reports. */
    val isValid: Boolean
        get() = rightPx > leftPx && bottomPx > topPx
}

/** Resolved horizontal slice of the status bar used as the tap zone. */
data class StatusBarZoneFrame(
    val widthPx: Int,
    val xPx: Int
)

/** Pure geometry for the "camera area" status-bar tap zone, centered on the display cutout. */
object TapLockStatusBarZone {
    const val CAMERA_AREA_FALLBACK_WIDTH_DP = 96
    const val CAMERA_AREA_MIN_WIDTH_DP = 72
    const val CAMERA_AREA_HORIZONTAL_PADDING_DP = 24

    /**
     * Computes the tap zone around the camera cutout: the union of cutouts intersecting the
     * status bar, padded and clamped to the screen. Falls back to a fixed-width centered zone on
     * devices with no cutout (e.g. emulators, punch-hole-less screens).
     */
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

    /** Converts [valueDp] to pixels at [density], never returning less than 1. */
    private fun dpToPx(valueDp: Int, density: Float): Int =
        (valueDp * density).roundToInt().coerceAtLeast(1)
}
