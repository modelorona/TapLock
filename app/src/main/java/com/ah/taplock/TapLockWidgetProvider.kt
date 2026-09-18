package com.ah.taplock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import java.io.File

/**
 * 1x1 home-screen widget that locks the device on tap. Built on RemoteViews (not Compose/Glance):
 * taps arrive as self-addressed broadcasts, and double-tap state lives in the companion because
 * the provider instance is recreated per broadcast.
 */
class TapLockWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_WIDGET_TAP = "com.ah.taplock.widget.TAP"
        // Static so the tap window survives across provider instances (one per broadcast).
        private val doubleTapDetector = DoubleTapDetector()

        /** Number of TapLock widgets currently placed on the home screen. */
        fun getWidgetCount(context: Context): Int =
            AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, TapLockWidgetProvider::class.java)
            ).size

        /**
         * Forces every placed widget to re-render by broadcasting ACTION_APPWIDGET_UPDATE. Needed
         * after style/icon preference changes because RemoteViews cache the last layout.
         */
        fun refreshAll(context: Context) {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, TapLockWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val intent = Intent(context, TapLockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    /** Renders every widget in [appWidgetIds] with the current preferences. */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /** Routes the self-addressed [ACTION_WIDGET_TAP] broadcast to the tap handler. */
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

    /**
     * Builds and pushes the RemoteViews for one widget: background style, optional icon (custom
     * PNG from app files or the launcher icon), and the tap PendingIntent.
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(context.getString(R.string.shared_pref_name), Context.MODE_PRIVATE)
        val showIcon = prefs.getBoolean(context.getString(R.string.show_widget_icon), false)
        val rippleEnabled = prefs.getBoolean(context.getString(R.string.widget_ripple_enabled), true)
        val widgetStyle = TapLockWidgetStyle.fromStored(
            prefs.getString(context.getString(R.string.widget_style), null)
        )

        // RemoteViews can't swap a view's foreground at runtime, so the ripple toggle picks
        // between two otherwise identical layouts.
        val layoutResId = if (rippleEnabled) R.layout.widget_layout else R.layout.widget_layout_no_ripple
        val views = RemoteViews(context.packageName, layoutResId).apply {
            setOnClickPendingIntent(
                R.id.widget_container,
                getPendingSelfIntent(context, appWidgetId)
            )
            setInt(R.id.widget_container, "setBackgroundResource", widgetStyle.backgroundResId)
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

    /**
     * Applies the widget tap mode (single/double) and, once triggered, locks the screen unless
     * the foreground app is excluded. Prefers the live service instance, then the root shell,
     * then a startService fallback when the service is enabled but not yet bound.
     */
    private fun handleWidgetTap(context: Context) {
        val prefs = context.getSharedPreferences(context.getString(R.string.shared_pref_name), Context.MODE_PRIVATE)
        val mode = TapZoneMode.fromStored(
            prefs.getString(context.getString(R.string.widget_mode), null),
            default = TapZoneMode.DOUBLE_TAP
        )
        val timeout = prefs.getInt(context.getString(R.string.double_tap_timeout), 300)

        if (mode == TapZoneMode.SINGLE_TAP || doubleTapDetector.onTap(timeout)) {
            val service = TapLockAccessibilityService.instance
            val isExcluded = service?.isForegroundAppExcludedNow()
                ?: TapLockAppRules.isCurrentAppExcluded(context)
            if (isExcluded) {
                TapLockFeedback.showAppExcluded(context)
                return
            }

            val vibrateOnLock = prefs.getBoolean(context.getString(R.string.vibrate_on_lock), true)
            val lockDelay = prefs.getInt(context.getString(R.string.lock_delay_ms), 0).toLong()
            if (vibrateOnLock) {
                VibrationHelper.vibrate(context, VibrationHelper.fromPrefs(context))
            }

            val lockRunnable = Runnable {
                if (service != null) {
                    Log.d("TapLock", "Widget: Using direct instance - fast path")
                    service.lockScreen()
                } else if (RootLock.isRootLockEnabled(context)) {
                    Log.d("TapLock", "Widget: No service - locking via root")
                    RootLock.performRootLock(context) { success ->
                        if (!success) TapLockFeedback.showRootLockFailed(context)
                    }
                } else if (isAccessibilityEnabled(context)) {
                    Log.d("TapLock", "Widget: Instance null - using slow startService path")
                    Toast.makeText(context, R.string.locking_screen, Toast.LENGTH_SHORT).show()
                    val accessibilityIntent = Intent(context, TapLockAccessibilityService::class.java)
                    accessibilityIntent.action = Intent.ACTION_SCREEN_OFF
                    context.startService(accessibilityIntent)
                } else {
                    TapLockFeedback.showAccessibilityRequired(context)
                }
            }

            // Leave the 100 ms vibration lead-in so the haptic is felt before the screen dies.
            val totalDelay = (if (vibrateOnLock) 100L else 0L) + lockDelay
            if (totalDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed(lockRunnable, totalDelay)
            } else {
                lockRunnable.run()
            }
        }
    }

    /** PendingIntent that routes a tap on widget [appWidgetId] back to this provider. */
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
