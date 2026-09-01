package com.ah.taplock

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.edit

/**
 * Quick Settings tile that locks the screen on tap. Mirrors the widget's lock strategy: live
 * service instance first, root shell second, startService fallback, otherwise point the user at
 * accessibility settings. Also tracks whether the tile is currently added, for the settings UI.
 */
class TapLockTileService : TileService() {

    /** Records that the tile was added and syncs its visual state. */
    override fun onTileAdded() {
        super.onTileAdded()
        persistTileAdded(true)
        updateTileState()
    }

    /** Records that the tile was removed from Quick Settings. */
    override fun onTileRemoved() {
        persistTileAdded(false)
        super.onTileRemoved()
    }

    /** Refreshes tile state each time the Quick Settings panel becomes visible. */
    override fun onStartListening() {
        super.onStartListening()
        persistTileAdded(true)
        updateTileState()
    }

    /**
     * Locks the screen unless the foreground app is excluded, applying the configured vibration
     * and lock delay. Falls back through root and startService paths when the service instance
     * is unavailable, and opens accessibility settings as a last resort.
     */
    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE)
        val service = TapLockAccessibilityService.instance
        val isExcluded = service?.isForegroundAppExcludedNow()
            ?: TapLockAppRules.isCurrentAppExcluded(this)
        if (isExcluded) {
            TapLockFeedback.showAppExcluded(this)
            return
        }

        val vibrateOnLock = prefs.getBoolean(getString(R.string.vibrate_on_lock), true)
        val lockDelay = prefs.getInt(getString(R.string.lock_delay_ms), 0).toLong()
        if (vibrateOnLock) {
            VibrationHelper.vibrate(this, VibrationHelper.fromPrefs(this))
        }

        if (service != null) {
            val totalDelay = (if (vibrateOnLock) 100L else 0L) + lockDelay
            if (totalDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed({ service.lockScreen() }, totalDelay)
            } else {
                service.lockScreen()
            }
        } else if (RootLock.isRootLockEnabled(this)) {
            val lockViaRoot = Runnable {
                RootLock.performRootLock(this) { success ->
                    if (!success) TapLockFeedback.showRootLockFailed(this)
                }
            }
            val totalDelay = (if (vibrateOnLock) 100L else 0L) + lockDelay
            if (totalDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed(lockViaRoot, totalDelay)
            } else {
                lockViaRoot.run()
            }
        } else {
            // Fallback checking if enabled
            if (isAccessibilityEnabled(this)) {
                // If instance is null but we think it's enabled, try starting it
                // This might happen if the service was killed or hasn't bound yet
                Toast.makeText(this, R.string.locking_screen, Toast.LENGTH_SHORT).show()
                val accessibilityIntent = Intent(this, TapLockAccessibilityService::class.java)
                accessibilityIntent.action = Intent.ACTION_SCREEN_OFF
                startService(accessibilityIntent)
            } else {
                TapLockFeedback.showAccessibilityRequired(this)
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                    startActivityAndCollapse(intent)
                }
            }
        }
    }
    
    /** Updates label, icon, and availability: usable when either lock method is ready. */
    private fun updateTileState() {
        qsTile?.let { tile ->
            val isEnabled = isAccessibilityEnabled(this) || RootLock.isRootLockEnabled(this)

            // STATE_INACTIVE is the correct state for a button that performs an action but doesn't have an on/off state.
            // It will appear white/grey (depending on theme) but not "highlighted/accented" like an active toggle.
            tile.state = if (isEnabled) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE

            tile.label = getString(R.string.tile_label)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_lock_tile)

            tile.updateTile()
        }
    }

    /** Persists [isAdded] so the settings screen can show whether the tile is in Quick Settings. */
    private fun persistTileAdded(isAdded: Boolean) {
        getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE)
            .edit { putBoolean(getString(R.string.quick_settings_tile_added), isAdded) }
    }
}
