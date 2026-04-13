package com.ah.taplock

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingButtonService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_button"
        private const val NOTIFICATION_ID = 1
    }

    private var floatingView: View? = null
    private lateinit var windowManager: WindowManager

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

        val size = (48 * resources.displayMetrics.density).toInt()

        val customIconFile = java.io.File(filesDir, "custom_widget_icon.png")
        val button = ImageView(this).apply {
            when {
                customIconFile.exists() -> {
                    setImageBitmap(BitmapFactory.decodeFile(customIconFile.absolutePath))
                }
                else -> {
                    setImageDrawable(packageManager.getApplicationIcon(packageName))
                }
            }
            alpha = 0.7f
            elevation = 8f
        }

        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
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
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Tap — lock the screen
                        val service = TapLockAccessibilityService.instance
                        if (service != null) {
                            val prefs = getSharedPreferences(
                                getString(R.string.shared_pref_name), MODE_PRIVATE
                            )
                            val vibrateOnLock = prefs.getBoolean(getString(R.string.vibrate_on_lock), true)
                            if (vibrateOnLock) {
                                VibrationHelper.vibrate(this@FloatingButtonService, VibrationHelper.fromPrefs(this@FloatingButtonService))
                            }
                            service.lockScreen()
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
}
