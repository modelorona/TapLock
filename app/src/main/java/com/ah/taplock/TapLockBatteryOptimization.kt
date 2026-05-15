package com.ah.taplock

import android.content.Intent
import android.net.Uri
import android.provider.Settings

object TapLockBatteryOptimization {
    const val REQUEST_ACTION = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    const val SETTINGS_ACTION = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS

    fun packageUriString(packageName: String): String = "package:$packageName"

    fun requestIntent(packageName: String): Intent =
        Intent(REQUEST_ACTION).apply {
            data = Uri.parse(packageUriString(packageName))
        }

    fun settingsIntent(): Intent = Intent(SETTINGS_ACTION)
}
