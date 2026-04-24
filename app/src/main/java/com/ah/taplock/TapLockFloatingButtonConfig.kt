package com.ah.taplock

object TapLockFloatingButtonConfig {
    const val DEFAULT_SIZE_DP = 56
    const val MIN_SIZE_DP = 40
    const val MAX_SIZE_DP = 88

    const val DEFAULT_OPACITY_PERCENT = 78
    const val MIN_OPACITY_PERCENT = 35
    const val MAX_OPACITY_PERCENT = 100

    fun clampSizeDp(value: Int): Int = value.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)

    fun clampOpacityPercent(value: Int): Int =
        value.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT)
}
