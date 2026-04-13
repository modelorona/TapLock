package com.ah.taplock

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class TapLockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE)
        val vibrateOnLock = prefs.getBoolean(getString(R.string.vibrate_on_lock), true)
        val lockDelay = prefs.getInt(getString(R.string.lock_delay_ms), 0).toLong()
        if (vibrateOnLock) {
            VibrationHelper.vibrate(this, VibrationHelper.fromPrefs(this))
        }

        val service = TapLockAccessibilityService.instance
        if (service != null) {
            val totalDelay = (if (vibrateOnLock) 100L else 0L) + lockDelay
            if (totalDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed({ service.lockScreen() }, totalDelay)
            } else {
                service.lockScreen()
            }
        } else {
            // Fallback checking if enabled
            if (isAccessibilityEnabled(this)) {
                // If instance is null but we think it's enabled, try starting it
                // This might happen if the service was killed or hasn't bound yet
                Toast.makeText(this, "Locking screen...", Toast.LENGTH_SHORT).show()
                val accessibilityIntent = Intent(this, TapLockAccessibilityService::class.java)
                accessibilityIntent.action = Intent.ACTION_SCREEN_OFF
                startService(accessibilityIntent)
            } else {
                Toast.makeText(this, getString(R.string.accessibility_permission_required), Toast.LENGTH_LONG).show()
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
    
    private fun updateTileState() {
        qsTile?.let { tile ->
            val isEnabled = isAccessibilityEnabled(this)

            // STATE_INACTIVE is the correct state for a button that performs an action but doesn't have an on/off state.
            // It will appear white/grey (depending on theme) but not "highlighted/accented" like an active toggle.
            tile.state = if (isEnabled) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE

            tile.label = getString(R.string.tile_label)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_lock_tile)

            tile.updateTile()
        }
    }
}