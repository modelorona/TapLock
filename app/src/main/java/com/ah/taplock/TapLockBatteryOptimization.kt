package com.ah.taplock

import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Intent builders for the battery-optimization exemption flow (keeps the service alive). */
object TapLockBatteryOptimization {
    const val REQUEST_ACTION = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    const val SETTINGS_ACTION = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS

    /** Builds the `package:` URI string the settings actions expect for [packageName]. */
    fun packageUriString(packageName: String): String = "package:$packageName"

    /** Intent showing the system dialog that asks to exempt [packageName] from optimization. */
    fun requestIntent(packageName: String): Intent =
        Intent(REQUEST_ACTION).apply {
            data = Uri.parse(packageUriString(packageName))
        }

    /** Intent opening the full battery-optimization settings list as a manual fallback. */
    fun settingsIntent(): Intent = Intent(SETTINGS_ACTION)
}
