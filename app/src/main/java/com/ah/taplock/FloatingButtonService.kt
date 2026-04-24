package com.ah.taplock

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import kotlin.math.abs

class FloatingButtonService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_button"
        private const val NOTIFICATION_ID = 1
    }

    private var floatingView: View? = null
    private lateinit var windowManager: WindowManager
    private val sharedPrefs by lazy {
        getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        addFloatingButton()
    }

    override fun onDestroy() {
        removeFloatingButton()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.floating_button_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.floating_button_notification_title))
        .setContentText(getString(R.string.floating_button_notification_body))
        .setSmallIcon(R.drawable.ic_lock_tile)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    @SuppressLint("ClickableViewAccessibility")
    private fun addFloatingButton() {
        if (floatingView != null) return

        val size = dpToPx(
            TapLockFloatingButtonConfig.clampSizeDp(
                sharedPrefs.getInt(
                    getString(R.string.floating_button_size_dp),
                    TapLockFloatingButtonConfig.DEFAULT_SIZE_DP
                )
            )
        )
        val opacity = TapLockFloatingButtonConfig.clampOpacityPercent(
            sharedPrefs.getInt(
                getString(R.string.floating_button_opacity_percent),
                TapLockFloatingButtonConfig.DEFAULT_OPACITY_PERCENT
            )
        ) / 100f

        val customIconFile = java.io.File(filesDir, "custom_widget_icon.png")
        val iconPadding = (size * 0.16f).toInt()
        val button = ImageView(this).apply {
            setBackgroundResource(R.drawable.floating_button_bg)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            when {
                customIconFile.exists() -> {
                    setImageBitmap(BitmapFactory.decodeFile(customIconFile.absolutePath))
                }
                else -> {
                    setImageDrawable(packageManager.getApplicationIcon(packageName))
                }
            }
            alpha = opacity
            elevation = dpToPx(4).toFloat()
        }

        val initialBounds = getScreenBounds()
        val savedX = sharedPrefs.getInt(getString(R.string.floating_button_position_x), -1)
        val savedY = sharedPrefs.getInt(getString(R.string.floating_button_position_y), -1)

        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX >= 0) savedX.coerceIn(0, (initialBounds.width() - size).coerceAtLeast(0)) else 0
            y = if (savedY >= 0) savedY.coerceIn(0, (initialBounds.height() - size).coerceAtLeast(0)) else 200
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val currentBounds = getScreenBounds()
                        params.x = (initialX + dx.toInt()).coerceIn(
                            0,
                            (currentBounds.width() - size).coerceAtLeast(0)
                        )
                        params.y = (initialY + dy.toInt()).coerceIn(
                            0,
                            (currentBounds.height() - size).coerceAtLeast(0)
                        )
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        val currentBounds = getScreenBounds()
                        params.x = if (params.x + (size / 2) < currentBounds.width() / 2) {
                            0
                        } else {
                            (currentBounds.width() - size).coerceAtLeast(0)
                        }
                        params.y = params.y.coerceIn(
                            0,
                            (currentBounds.height() - size).coerceAtLeast(0)
                        )
                        windowManager.updateViewLayout(floatingView, params)
                        persistPosition(params.x, params.y)
                    } else {
                        val service = TapLockAccessibilityService.instance
                        val isExcluded = service?.isForegroundAppExcludedNow()
                            ?: TapLockAppRules.isCurrentAppExcluded(this@FloatingButtonService)
                        if (isExcluded) {
                            TapLockFeedback.showAppExcluded(this@FloatingButtonService)
                            v.performClick()
                            return@setOnTouchListener true
                        }

                        if (service != null) {
                            val prefs = getSharedPreferences(
                                getString(R.string.shared_pref_name), MODE_PRIVATE
                            )
                            val vibrateOnLock = prefs.getBoolean(getString(R.string.vibrate_on_lock), true)
                            if (vibrateOnLock) {
                                VibrationHelper.vibrate(this@FloatingButtonService, VibrationHelper.fromPrefs(this@FloatingButtonService))
                            }
                            service.lockScreen()
                        } else if (isAccessibilityEnabled(this@FloatingButtonService)) {
                            Toast.makeText(
                                this@FloatingButtonService,
                                "Locking screen...",
                                Toast.LENGTH_SHORT
                            ).show()
                            val accessibilityIntent = Intent(
                                this@FloatingButtonService,
                                TapLockAccessibilityService::class.java
                            ).apply {
                                action = Intent.ACTION_SCREEN_OFF
                            }
                            startService(accessibilityIntent)
                        } else {
                            TapLockFeedback.showAccessibilityRequired(this@FloatingButtonService)
                        }
                    }
                    v.performClick()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(button, params)
        floatingView = button
    }

    private fun removeFloatingButton() {
        floatingView?.let {
            windowManager.removeView(it)
        }
        floatingView = null
    }

    private fun getScreenBounds(): Rect = windowManager.currentWindowMetrics.bounds

    private fun persistPosition(x: Int, y: Int) {
        sharedPrefs.edit {
            putInt(getString(R.string.floating_button_position_x), x)
            putInt(getString(R.string.floating_button_position_y), y)
        }
    }

    private fun dpToPx(valueDp: Int): Int =
        (valueDp * resources.displayMetrics.density).toInt()
}
