package com.ah.taplock

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator

enum class VibrationPattern(val durationMs: Long, val amplitude: Int) {
    LIGHT(30, 40),
    MEDIUM(50, 80),
    STRONG(80, 255)
}

object VibrationHelper {

    fun fromPrefs(context: Context): VibrationPattern {
        val prefs = context.getSharedPreferences(
            context.getString(R.string.shared_pref_name), Context.MODE_PRIVATE
        )
        val name = prefs.getString(context.getString(R.string.vibration_pattern), null)
        return try {
            if (name != null) VibrationPattern.valueOf(name) else VibrationPattern.MEDIUM
        } catch (_: IllegalArgumentException) {
            VibrationPattern.MEDIUM
        }
    }

    fun vibrate(context: Context, pattern: VibrationPattern) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        val effect = VibrationEffect.createOneShot(pattern.durationMs, pattern.amplitude)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, attrs)
        }
    }
}
