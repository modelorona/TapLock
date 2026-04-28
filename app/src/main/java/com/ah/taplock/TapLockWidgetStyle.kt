package com.ah.taplock

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class TapLockWidgetStyle(
    @field:StringRes val labelResId: Int,
    @field:DrawableRes val backgroundResId: Int
) {
    TRANSPARENT(
        R.string.widget_style_transparent,
        R.drawable.widget_bg_transparent
    ),
    GLASS(
        R.string.widget_style_glass,
        R.drawable.widget_bg_glass
    ),
    SOLID(
        R.string.widget_style_solid,
        R.drawable.widget_bg_solid
    ),
    OUTLINE(
        R.string.widget_style_outline,
        R.drawable.widget_bg_outline
    );

    companion object {
        val default = TRANSPARENT

        fun fromStored(value: String?): TapLockWidgetStyle =
            entries.firstOrNull { it.name == value } ?: default
    }
}
