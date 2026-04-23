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
import kotlin.math.abs

class TapLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TapLock"
        @Suppress("StaticFieldLeak") // Intentional: cleared in onUnbind/onDestroy
        var instance: TapLockAccessibilityService? = null
            private set
    }

    private var statusBarOverlay: View? = null
    private var leftEdgeOverlay: View? = null
    private var rightEdgeOverlay: View? = null
    private val doubleTapDetector = DoubleTapDetector()
    private val leftEdgeDoubleTapDetector = DoubleTapDetector()
    private val rightEdgeDoubleTapDetector = DoubleTapDetector()
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var isOnLockScreen = false
    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        currentForegroundPackage = TapLockAppRules.getForegroundPackage(this)
        serviceInfo?.let { info ->
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.eventTypes = info.eventTypes or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            serviceInfo = info
        }
        updateOverlay()
        registerPrefListener()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        removeStatusBarOverlay()
        removeEdgeOverlays()
        unregisterPrefListener()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        removeStatusBarOverlay()
        removeEdgeOverlays()
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

    fun isForegroundAppExcludedNow(): Boolean {
        refreshForegroundPackage()
        return isCurrentAppExcluded()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (refreshForegroundPackage(event.packageName?.toString())) {
            updateOverlayTouchability()
        }

        if (
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
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
                getString(R.string.left_edge_lock_zone),
                getString(R.string.right_edge_lock_zone),
                getString(R.string.edge_zone_width_dp),
                getString(R.string.edge_zone_top_offset_percent),
                getString(R.string.edge_zone_bottom_offset_percent) -> updateEdgeOverlays()
                getString(R.string.excluded_apps) -> updateOverlayTouchability()
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
        updateStatusBarOverlay()
        updateEdgeOverlays()
    }

    private fun updateStatusBarOverlay() {
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

    private fun updateEdgeOverlays() {
        val prefs = getPrefs()
        val leftEnabled = prefs.getBoolean(getString(R.string.left_edge_lock_zone), false)
        val rightEnabled = prefs.getBoolean(getString(R.string.right_edge_lock_zone), false)
        val canShowEdgeZones = shouldShowEdgeZones()

        if (leftEnabled && canShowEdgeZones) {
            if (leftEdgeOverlay == null) addEdgeOverlay(EdgeZoneSide.LEFT)
            updateEdgeOverlayLayout(EdgeZoneSide.LEFT)
        } else {
            removeEdgeOverlay(EdgeZoneSide.LEFT)
        }

        if (rightEnabled && canShowEdgeZones) {
            if (rightEdgeOverlay == null) addEdgeOverlay(EdgeZoneSide.RIGHT)
            updateEdgeOverlayLayout(EdgeZoneSide.RIGHT)
        } else {
            removeEdgeOverlay(EdgeZoneSide.RIGHT)
        }

        updateEdgeOverlayTouchability()
    }

    private fun addEdgeOverlay(side: EdgeZoneSide) {
        if (getEdgeOverlay(side) != null) {
            return
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
        val detector = getEdgeDetector(side)

        var downTimeMs = 0L
        var downX = 0f
        var downY = 0f
        var gestureHandled = false

        val overlay = View(this).apply {
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTimeMs = System.currentTimeMillis()
                        downX = event.rawX
                        downY = event.rawY
                        gestureHandled = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!gestureHandled) {
                            val dx = event.rawX - downX
                            val dy = event.rawY - downY
                            if (shouldTriggerBackSwipe(side, dx, dy, touchSlop)) {
                                gestureHandled = true
                                detector.reset()
                                performGlobalAction(GLOBAL_ACTION_BACK)
                            } else if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                                gestureHandled = true
                                detector.reset()
                            }
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!gestureHandled) {
                            handleEdgeTap(side, downTimeMs)
                        }
                        v.performClick()
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        detector.reset()
                    }
                }
                true
            }
        }

        val frame = createEdgeFrame(side)
        val params = WindowManager.LayoutParams(
            frame.widthPx,
            frame.heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            createOverlayBaseFlags(),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = frame.x
            y = frame.y
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        wm.addView(overlay, params)
        setEdgeOverlay(side, overlay)
        Log.d(TAG, "edgeOverlay: added side=$side, frame=$frame")
    }

    private fun updateEdgeOverlayLayout(side: EdgeZoneSide) {
        val overlay = getEdgeOverlay(side) ?: return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = overlay.layoutParams as WindowManager.LayoutParams
        val frame = createEdgeFrame(side)

        if (
            params.width != frame.widthPx ||
            params.height != frame.heightPx ||
            params.x != frame.x ||
            params.y != frame.y
        ) {
            params.width = frame.widthPx
            params.height = frame.heightPx
            params.x = frame.x
            params.y = frame.y
            wm.updateViewLayout(overlay, params)
            getEdgeDetector(side).reset()
            Log.d(TAG, "edgeOverlay: updated side=$side, frame=$frame")
        }
    }

    private fun updateEdgeOverlayTouchability() {
        val touchDisabled = isCurrentAppExcluded() || isTapLockForeground()
        updateEdgeOverlayTouchability(EdgeZoneSide.LEFT, touchDisabled)
        updateEdgeOverlayTouchability(EdgeZoneSide.RIGHT, touchDisabled)
    }

    private fun updateEdgeOverlayTouchability(side: EdgeZoneSide, touchDisabled: Boolean) {
        val overlay = getEdgeOverlay(side) ?: return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = overlay.layoutParams as WindowManager.LayoutParams
        val newFlags = if (touchDisabled) {
            createOverlayBaseFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            createOverlayBaseFlags()
        }

        if (params.flags != newFlags) {
            params.flags = newFlags
            wm.updateViewLayout(overlay, params)
            if (touchDisabled) {
                getEdgeDetector(side).reset()
            }
            Log.d(TAG, "edgeOverlay: touchDisabled=$touchDisabled, side=$side")
        }
    }

    private fun updateOverlayTouchability() {
        refreshForegroundPackage()
        updateStatusBarOverlayTouchability()
        updateEdgeOverlayTouchability()
    }

    private fun updateStatusBarOverlayTouchability() {
        val overlay = statusBarOverlay ?: return
        val prefs = getPrefs()
        val statusBarEnabled = prefs.getBoolean(getString(R.string.status_bar_double_tap), false)
        val lockScreenEnabled = prefs.getBoolean(getString(R.string.lock_screen_double_tap), false)
        val appExcluded = isCurrentAppExcluded()

        // Disable touch when: in a fullscreen app (no status bar) and not on lock screen,
        // when the current app is excluded, or when only lock screen feature is enabled
        // and we're not on the lock screen.
        val shouldDisableTouch = when {
            isOnLockScreen && lockScreenEnabled -> false
            appExcluded -> true
            !isStatusBarVisible() -> true
            statusBarEnabled -> false
            else -> true // lock screen only, but not on lock screen
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = overlay.layoutParams as WindowManager.LayoutParams
        val baseFlags = createOverlayBaseFlags()
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
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val locked = km.isKeyguardLocked
        val wasOnLockScreen = isOnLockScreen
        isOnLockScreen = locked

        val overlay = statusBarOverlay
        if (overlay == null) {
            if (locked != wasOnLockScreen) {
                updateOverlayTouchability()
            }
            updateEdgeOverlays()
            return
        }

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

        updateEdgeOverlays()
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

    private fun removeEdgeOverlays() {
        removeEdgeOverlay(EdgeZoneSide.LEFT)
        removeEdgeOverlay(EdgeZoneSide.RIGHT)
    }

    private fun removeEdgeOverlay(side: EdgeZoneSide) {
        val overlay = getEdgeOverlay(side) ?: return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.removeView(overlay)
        setEdgeOverlay(side, null)
        getEdgeDetector(side).reset()
    }

    private fun handleStatusBarTap(tapTimeMs: Long) {
        refreshForegroundPackage()
        if (isCurrentAppExcluded()) {
            doubleTapDetector.reset()
            Log.d(TAG, "touch: ignoring tap because current app is excluded")
            return
        }

        maybeTriggerLock(doubleTapDetector, tapTimeMs, "statusBar")
    }

    private fun handleEdgeTap(side: EdgeZoneSide, tapTimeMs: Long) {
        refreshForegroundPackage()
        val detector = getEdgeDetector(side)
        if (isCurrentAppExcluded() || isTapLockForeground()) {
            detector.reset()
            Log.d(TAG, "edgeOverlay: ignoring tap because edge interaction is suppressed, side=$side")
            return
        }

        maybeTriggerLock(detector, tapTimeMs, "edge:$side")
    }

    private fun maybeTriggerLock(
        detector: DoubleTapDetector,
        tapTimeMs: Long,
        source: String
    ) {
        val prefs = getPrefs()
        val timeout = prefs.getInt(getString(R.string.double_tap_timeout), 300)
        Log.d(TAG, "$source: tap registered, timeout=${timeout}ms")

        if (detector.onTap(timeout, tapTimeMs)) {
            Log.d(TAG, "$source: DOUBLE TAP detected, locking")
            performConfiguredLock(prefs)
        }
    }

    private fun performConfiguredLock(prefs: SharedPreferences) {
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

    private fun createOverlayBaseFlags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    private fun createEdgeFrame(side: EdgeZoneSide): EdgeZoneFrame {
        val prefs = getPrefs()
        val widthDp = prefs.getInt(
            getString(R.string.edge_zone_width_dp),
            TapLockEdgeZones.DEFAULT_WIDTH_DP
        )
        val legacyCoveragePercent = prefs.getInt(
            getString(R.string.edge_zone_coverage_percent),
            TapLockEdgeZones.DEFAULT_COVERAGE_PERCENT
        )
        val (fallbackTopOffsetPercent, fallbackBottomOffsetPercent) =
            TapLockEdgeZones.deriveOffsetsFromCoverage(legacyCoveragePercent)
        val topOffsetPercent = prefs.getInt(
            getString(R.string.edge_zone_top_offset_percent),
            fallbackTopOffsetPercent
        )
        val bottomOffsetPercent = prefs.getInt(
            getString(R.string.edge_zone_bottom_offset_percent),
            fallbackBottomOffsetPercent
        )
        val bounds = (getSystemService(WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds
        return TapLockEdgeZones.buildFrame(
            screenWidthPx = bounds.width(),
            screenHeightPx = bounds.height(),
            widthDp = widthDp,
            density = resources.displayMetrics.density,
            topOffsetPercent = topOffsetPercent,
            bottomOffsetPercent = bottomOffsetPercent,
            side = side
        )
    }

    private fun shouldShowEdgeZones(): Boolean {
        val bounds = (getSystemService(WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds
        return TapLockEdgeZones.isPortrait(bounds.width(), bounds.height()) && !isDeviceLocked()
    }

    private fun isDeviceLocked(): Boolean =
        (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

    private fun getEdgeOverlay(side: EdgeZoneSide): View? = when (side) {
        EdgeZoneSide.LEFT -> leftEdgeOverlay
        EdgeZoneSide.RIGHT -> rightEdgeOverlay
    }

    private fun setEdgeOverlay(side: EdgeZoneSide, overlay: View?) {
        when (side) {
            EdgeZoneSide.LEFT -> leftEdgeOverlay = overlay
            EdgeZoneSide.RIGHT -> rightEdgeOverlay = overlay
        }
    }

    private fun getEdgeDetector(side: EdgeZoneSide): DoubleTapDetector = when (side) {
        EdgeZoneSide.LEFT -> leftEdgeDoubleTapDetector
        EdgeZoneSide.RIGHT -> rightEdgeDoubleTapDetector
    }

    private fun shouldTriggerBackSwipe(
        side: EdgeZoneSide,
        dx: Float,
        dy: Float,
        touchSlop: Int
    ): Boolean {
        val isHorizontalSwipe = abs(dx) > abs(dy)
        if (!isHorizontalSwipe) return false

        return when (side) {
            EdgeZoneSide.LEFT -> dx > touchSlop
            EdgeZoneSide.RIGHT -> dx < -touchSlop
        }
    }

    private fun isTapLockForeground(): Boolean {
        if (rootInActiveWindow?.packageName?.toString() == packageName) {
            return true
        }

        return windows
            .asSequence()
            .filter { window ->
                window.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                    (window.isActive || window.isFocused)
            }
            .mapNotNull { window -> window.root?.packageName?.toString() }
            .any { foregroundPackage -> foregroundPackage == packageName }
    }

    private fun isCurrentAppExcluded(): Boolean =
        !isOnLockScreen && TapLockAppRules.isPackageExcluded(this, currentForegroundPackage)

    private fun refreshForegroundPackage(eventPackage: String? = null): Boolean {
        val previousPackage = currentForegroundPackage
        val resolvedPackage = resolveForegroundPackage(eventPackage)

        if (resolvedPackage != null) {
            TapLockAppRules.updateForegroundPackage(this, resolvedPackage)
        }

        currentForegroundPackage = TapLockAppRules.getForegroundPackage(this)
        return currentForegroundPackage != previousPackage
    }

    private fun resolveForegroundPackage(eventPackage: String? = null): String? {
        val activeRootPackage = TapLockAppRules.sanitizeTrackedPackage(
            this,
            rootInActiveWindow?.packageName?.toString()
        )
        if (activeRootPackage != null) return activeRootPackage

        val windowPackage = windows
            .asSequence()
            .filter { window ->
                window.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                    (window.isActive || window.isFocused)
            }
            .mapNotNull { window -> window.root?.packageName?.toString() }
            .mapNotNull { packageName -> TapLockAppRules.sanitizeTrackedPackage(this, packageName) }
            .firstOrNull()
        if (windowPackage != null) return windowPackage

        return TapLockAppRules.sanitizeTrackedPackage(this, eventPackage)
            ?: currentForegroundPackage
    }
}
