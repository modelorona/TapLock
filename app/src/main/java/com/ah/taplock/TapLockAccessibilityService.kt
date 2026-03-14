package com.ah.taplock

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class TapLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TapLock"
        var instance: TapLockAccessibilityService? = null
            private set
    }

    private var statusBarOverlay: View? = null
    private var lastTapTime = 0L
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        updateStatusBarOverlay()
        registerPrefListener()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        removeStatusBarOverlay()
        unregisterPrefListener()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        removeStatusBarOverlay()
        unregisterPrefListener()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            lockScreen()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun getPrefs(): SharedPreferences {
        return getSharedPreferences(getString(R.string.shared_pref_name), Context.MODE_PRIVATE)
    }

    private fun registerPrefListener() {
        val prefs = getPrefs()
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == getString(R.string.status_bar_double_tap)) {
                updateStatusBarOverlay()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    private fun unregisterPrefListener() {
        prefListener?.let {
            getPrefs().unregisterOnSharedPreferenceChangeListener(it)
        }
        prefListener = null
    }

    private fun updateStatusBarOverlay() {
        val enabled = getPrefs().getBoolean(getString(R.string.status_bar_double_tap), false)
        Log.d(TAG, "updateStatusBarOverlay: enabled=$enabled")
        if (enabled) {
            addStatusBarOverlay()
        } else {
            removeStatusBarOverlay()
        }
    }

    private fun addStatusBarOverlay() {
        if (statusBarOverlay != null) {
            Log.d(TAG, "addStatusBarOverlay: already exists, skipping")
            return
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val overlay = View(this).apply {
            setOnTouchListener { v, event ->
                if (handleStatusBarTouch(event)) {
                    v.performClick()
                    true
                } else {
                    false
                }
            }
        }

        val statusBarHeight = getStatusBarHeight()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            statusBarHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        wm.addView(overlay, params)
        statusBarOverlay = overlay
        Log.d(TAG, "addStatusBarOverlay: added, height=${statusBarHeight}px")
    }

    private fun removeStatusBarOverlay() {
        statusBarOverlay?.let {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(it)
        }
        statusBarOverlay = null
        lastTapTime = 0L
    }

    private fun handleStatusBarTouch(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val prefs = getPrefs()
            val timeout = prefs.getInt(getString(R.string.double_tap_timeout), 300)
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastTapTime
            Log.d(TAG, "touch: ACTION_DOWN at y=${event.rawY}, elapsed=${elapsed}ms, timeout=${timeout}ms")

            if (elapsed < timeout) {
                Log.d(TAG, "touch: DOUBLE TAP detected, locking")
                lastTapTime = 0L

                val vibrateOnLock = prefs.getBoolean(getString(R.string.vibrate_on_lock), true)
                if (vibrateOnLock) {
                    val vibrator = getSystemService(Vibrator::class.java)
                    val effect = VibrationEffect.createOneShot(50, 80)
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
                    Handler(Looper.getMainLooper()).postDelayed({ lockScreen() }, 100)
                } else {
                    lockScreen()
                }
                return true
            }
            lastTapTime = currentTime
        }
        return false
    }

    private fun getStatusBarHeight(): Int {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val insets = wm.currentWindowMetrics.windowInsets
            .getInsets(WindowInsets.Type.statusBars())
        if (insets.top > 0) {
            Log.d(TAG, "getStatusBarHeight: ${insets.top}px (from WindowInsets)")
            return insets.top
        }
        val fallback = (24 * resources.displayMetrics.density).toInt()
        Log.w(TAG, "getStatusBarHeight: insets.top was 0, using fallback ${fallback}px")
        return fallback
    }
}
