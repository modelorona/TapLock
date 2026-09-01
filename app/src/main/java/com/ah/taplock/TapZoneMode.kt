package com.ah.taplock

import androidx.annotation.StringRes

/** How a tap zone triggers the lock: disabled, on a double tap, or on a single tap. */
enum class TapZoneMode(@field:StringRes val labelResId: Int) {
    OFF(R.string.tap_mode_off),
    DOUBLE_TAP(R.string.tap_mode_double_tap),
    SINGLE_TAP(R.string.tap_mode_single_tap);

    companion object {
        val default = OFF

        /** Parses a stored preference [value], tolerating null or stale names via [default]. */
        fun fromStored(value: String?, default: TapZoneMode = OFF): TapZoneMode =
            entries.firstOrNull { it.name == value } ?: default
    }
}
