package com.ah.taplock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.edit

class TapLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TapLock"
        @Suppress("StaticFieldLeak") // Intentional: cleared in onUnbind/onDestroy
        var instance: TapLockAccessibilityService? = null
            private set
    }

    private var statusBarOverlay: View? = null
    private val doubleTapDetector = DoubleTapDetector()
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var isOnLockScreen = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo?.let { info ->
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.eventTypes = info.eventTypes or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            serviceInfo = info
        }
        updateOverlay()
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
        val prefs = getPrefs()
        val count = prefs.getInt(getString(R.string.lock_count), 0)
        prefs.edit { putInt(getString(R.string.lock_count), count + 1) }
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            updateOverlayTouchability()
            updateOverlayForLockScreen()
        }
    }

    override fun onInterrupt() {}

    private fun getPrefs(): SharedPreferences {
        return getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE)
    }

    private fun registerPrefListener() {
        val prefs = getPrefs()
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                getString(R.string.status_bar_double_tap),
                getString(R.string.lock_screen_double_tap) -> updateOverlay()
                getString(R.string.lock_zone_percent) -> {
                    if (statusBarOverlay != null) updateOverlayForLockScreen()
                }
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

    private fun updateOverlay() {
        val prefs = getPrefs()
        val statusBarEnabled = prefs.getBoolean(getString(R.string.status_bar_double_tap), false)
        val lockScreenEnabled = prefs.getBoolean(getString(R.string.lock_screen_double_tap), false)
        val needsOverlay = statusBarEnabled || lockScreenEnabled
        Log.d(TAG, "updateOverlay: statusBar=$statusBarEnabled, lockScreen=$lockScreenEnabled")

        if (needsOverlay && statusBarOverlay == null) {
            addStatusBarOverlay()
        } else if (!needsOverlay && statusBarOverlay != null) {
            removeStatusBarOverlay()
        }

        // Update size if overlay exists
        if (statusBarOverlay != null) {
            updateOverlayForLockScreen()
        }
    }

    private fun addStatusBarOverlay() {
        if (statusBarOverlay != null) {
            Log.d(TAG, "addStatusBarOverlay: already exists, skipping")
            return
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        var downTimeMs = 0L
        var downX = 0f
        var downY = 0f
        var swiped = false
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        val overlay = View(this).apply {
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTimeMs = System.currentTimeMillis()
                        downX = event.rawX
                        downY = event.rawY
                        swiped = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!swiped) {
                            val dy = event.rawY - downY
                            if (isOnLockScreen && dy < -touchSlop) {
                                // Swipe up on lock screen — dismiss keyguard
                                swiped = true
                                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                            } else if (!isOnLockScreen && dy > touchSlop) {
                                swiped = true
                                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!swiped) {
                            if (isOnLockScreen) {
                                handleLockScreenTap(downTimeMs, downX, downY)
                            } else if (isStatusBarVisible()) {
                                handleStatusBarTap(downTimeMs)
                            } else {
                                doubleTapDetector.reset()
                            }
                        }
                        v.performClick()
                    }
                }
                true
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

    private fun updateOverlayTouchability() {
        val overlay = statusBarOverlay ?: return
        val prefs = getPrefs()
        val statusBarEnabled = prefs.getBoolean(getString(R.string.status_bar_double_tap), false)
        val lockScreenEnabled = prefs.getBoolean(getString(R.string.lock_screen_double_tap), false)

        // Disable touch when: in a fullscreen app (no status bar) and not on lock screen,
        // or when only lock screen feature is enabled and we're not on the lock screen
        val shouldDisableTouch = when {
            isOnLockScreen && lockScreenEnabled -> false
            !isStatusBarVisible() -> true
            statusBarEnabled -> false
            else -> true // lock screen only, but not on lock screen
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = overlay.layoutParams as WindowManager.LayoutParams
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val newFlags = if (shouldDisableTouch) {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            baseFlags
        }
        if (params.flags != newFlags) {
            params.flags = newFlags
            wm.updateViewLayout(overlay, params)
            if (shouldDisableTouch) doubleTapDetector.reset()
            Log.d(TAG, "overlay: touchDisabled=$shouldDisableTouch, lockScreen=$isOnLockScreen")
        }
    }

    private fun updateOverlayForLockScreen() {
        val overlay = statusBarOverlay ?: return
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val locked = km.isKeyguardLocked
        val wasOnLockScreen = isOnLockScreen
        isOnLockScreen = locked

        val prefs = getPrefs()
        val lockScreenEnabled = prefs.getBoolean(
            getString(R.string.lock_screen_double_tap), false
        )
        val statusBarEnabled = prefs.getBoolean(
            getString(R.string.status_bar_double_tap), false
        )

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = overlay.layoutParams as WindowManager.LayoutParams
        val newHeight = if (locked && lockScreenEnabled) {
            getLockScreenOverlayHeight()
        } else if (statusBarEnabled) {
            getStatusBarHeight()
        } else {
            // Only lock screen feature enabled, not on lock screen — keep minimal
            getStatusBarHeight()
        }

        if (params.height != newHeight) {
            params.height = newHeight
            wm.updateViewLayout(overlay, params)
            doubleTapDetector.reset()
            Log.d(TAG, "overlay: lockScreen=$locked, height=${newHeight}px")
        }

        if (locked != wasOnLockScreen) {
            updateOverlayTouchability()
        }
    }

    private fun getLockScreenOverlayHeight(): Int {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val screenHeight = wm.currentWindowMetrics.bounds.height()
        val percent = getPrefs().getInt(getString(R.string.lock_zone_percent), 66)
        return (screenHeight * percent) / 100
    }

    private fun handleLockScreenTap(tapTimeMs: Long, x: Float, y: Float) {
        val clickedNode = findClickableNodeAt(x.toInt(), y.toInt())
        if (clickedNode != null) {
            Log.d(TAG, "lock screen tap: forwarding click to interactive element")
            clickedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            doubleTapDetector.reset()
            return
        }
        // Empty space — count toward double-tap
        handleStatusBarTap(tapTimeMs)
    }

    private fun findClickableNodeAt(x: Int, y: Int): AccessibilityNodeInfo? {
        val nodeRect = Rect()
        for (window in windows) {
            // Skip our own overlay
            if (window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) continue
            val windowRect = Rect()
            window.getBoundsInScreen(windowRect)
            if (!windowRect.contains(x, y)) continue

            val root = window.root ?: continue
            val result = findDeepestClickableNode(root, x, y, nodeRect)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun findDeepestClickableNode(
        node: AccessibilityNodeInfo,
        x: Int,
        y: Int,
        rect: Rect
    ): AccessibilityNodeInfo? {
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null

        // Check children first (deeper = more specific)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findDeepestClickableNode(child, x, y, rect)
            if (result != null) {
                return result
            }
        }

        // This node contains the point — is it clickable?
        if (node.isClickable) {
            return node
        }
        return null
    }

    private fun isStatusBarVisible(): Boolean {
        val allWindows = windows
        if (allWindows.isEmpty()) return true
        val rect = Rect()
        return allWindows.any { window ->
            window.type == AccessibilityWindowInfo.TYPE_SYSTEM &&
                rect.also { window.getBoundsInScreen(it) }.let {
                    it.top == 0 && it.height() > 0
                }
        }
    }

    private fun removeStatusBarOverlay() {
        statusBarOverlay?.let {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(it)
        }
        statusBarOverlay = null
        doubleTapDetector.reset()
    }

    private fun handleStatusBarTap(tapTimeMs: Long) {
        val prefs = getPrefs()
        val timeout = prefs.getInt(getString(R.string.double_tap_timeout), 300)
        Log.d(TAG, "touch: tap registered, timeout=${timeout}ms")

        if (doubleTapDetector.onTap(timeout, tapTimeMs)) {
            Log.d(TAG, "touch: DOUBLE TAP detected, locking")

            val vibrateOnLock = prefs.getBoolean(getString(R.string.vibrate_on_lock), true)
            val lockDelay = prefs.getInt(getString(R.string.lock_delay_ms), 0).toLong()
            if (vibrateOnLock) {
                VibrationHelper.vibrate(this, VibrationHelper.fromPrefs(this))
                Handler(Looper.getMainLooper()).postDelayed({ lockScreen() }, 100 + lockDelay)
            } else if (lockDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed({ lockScreen() }, lockDelay)
            } else {
                lockScreen()
            }
        }
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
