package com.ah.taplock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import java.io.File

class TapLockWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_WIDGET_TAP = "com.ah.taplock.widget.TAP"
        private var lastTapTime = 0L
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_WIDGET_TAP -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    handleWidgetTap(context)
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(context.getString(R.string.shared_pref_name), Context.MODE_PRIVATE)
        val showIcon = prefs.getBoolean(context.getString(R.string.show_widget_icon), false)

        val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
            setOnClickPendingIntent(
                R.id.widget_container,
                getPendingSelfIntent(context, appWidgetId)
            )
            if (showIcon) {
                val customIconFile = File(context.filesDir, "custom_widget_icon.png")
                if (customIconFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(customIconFile.absolutePath)
                    setImageViewBitmap(R.id.widget_icon, bitmap)
                } else {
                    setImageViewResource(R.id.widget_icon, R.mipmap.ic_launcher)
                }
                setViewVisibility(R.id.widget_icon, View.VISIBLE)
            } else {
                setViewVisibility(R.id.widget_icon, View.GONE)
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun handleWidgetTap(context: Context) {
        val prefs = context.getSharedPreferences(context.getString(R.string.shared_pref_name), Context.MODE_PRIVATE)
        val timeout = prefs.getInt(context.getString(R.string.double_tap_timeout), 300)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < timeout) {
            // Haptic feedback if enabled
            val vibrateOnLock = prefs.getBoolean(context.getString(R.string.vibrate_on_lock), true)
            if (vibrateOnLock) {
                val vibrator = context.getSystemService(Vibrator::class.java)
                val effect = VibrationEffect.createOneShot(50, 80)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, attrs)
                } else {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, attrs)
                }
            }

            // Delay lock slightly so the haptic feedback can complete
            val lockRunnable = Runnable {
                val service = TapLockAccessibilityService.instance
                if (service != null) {
                    Log.d("TapLock", "Widget: Using direct instance - fast path")
                    service.lockScreen()
                } else {
                    Log.d("TapLock", "Widget: Instance null - using slow startService path")
                    Toast.makeText(context, "Locking screen...", Toast.LENGTH_SHORT).show()
                    val accessibilityIntent = Intent(context, TapLockAccessibilityService::class.java)
                    accessibilityIntent.action = Intent.ACTION_SCREEN_OFF
                    context.startService(accessibilityIntent)
                }
            }

            if (vibrateOnLock) {
                Handler(Looper.getMainLooper()).postDelayed(lockRunnable, 100)
            } else {
                lockRunnable.run()
            }
        }
        lastTapTime = currentTime
    }

    private fun getPendingSelfIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, TapLockWidgetProvider::class.java).apply {
            this.action = ACTION_WIDGET_TAP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}