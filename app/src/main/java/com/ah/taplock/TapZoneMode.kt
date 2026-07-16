package com.ah.taplock

import androidx.annotation.StringRes

enum class TapZoneMode(@field:StringRes val labelResId: Int) {
    OFF(R.string.tap_mode_off),
    DOUBLE_TAP(R.string.tap_mode_double_tap),
    SINGLE_TAP(R.string.tap_mode_single_tap);

    companion object {
        val default = OFF

        fun fromStored(value: String?, default: TapZoneMode = OFF): TapZoneMode =
            entries.firstOrNull { it.name == value } ?: default
    }
}
