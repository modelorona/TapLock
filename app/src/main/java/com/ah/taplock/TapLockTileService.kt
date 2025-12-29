package com.ah.taplock

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
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
        
        // Try direct call first for speed
        val service = TapLockAccessibilityService.instance
        if (service != null) {
            service.lockScreen()
        } else {
            // Fallback checking if enabled
            if (isAccessibilityEnabled(this)) {
                // If instance is null but we think it's enabled, try starting it
                // This might happen if the service was killed or hasn't bound yet
                val accessibilityIntent = Intent(this, TapLockAccessibilityService::class.java)
                accessibilityIntent.action = Intent.ACTION_SCREEN_OFF
                startService(accessibilityIntent)
            } else {
                Toast.makeText(this, getString(R.string.accessibility_permission_required), Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                if (Build.VERSION.SDK_INT >= 34) {
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
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