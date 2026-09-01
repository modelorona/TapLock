package com.ah.taplock

/** Size and opacity bounds for the floating lock button, shared by settings UI and overlay. */
object TapLockFloatingButtonConfig {
    const val DEFAULT_SIZE_DP = 56
    const val MIN_SIZE_DP = 40
    const val MAX_SIZE_DP = 88

    const val DEFAULT_OPACITY_PERCENT = 78
    const val MIN_OPACITY_PERCENT = 35
    const val MAX_OPACITY_PERCENT = 100

    /** Clamps a button size [value] (dp) into the supported range. */
    fun clampSizeDp(value: Int): Int = value.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)

    /** Clamps a button opacity [value] (percent) into the supported range. */
    fun clampOpacityPercent(value: Int): Int =
        value.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT)
}
