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
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        
        if (isAccessibilityEnabled(this)) {
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
    
    private fun updateTile() {
        qsTile?.let { tile ->
            // Update state based on accessibility enabled status
            // STATE_INACTIVE makes it look like a button (clickable but not "ON")
            // STATE_ACTIVE makes it look "ON" (colored)
            // STATE_UNAVAILABLE makes it greyed out
            
            val isEnabled = isAccessibilityEnabled(this)
            tile.state = if (isEnabled) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            
            // Ensure label and icon are set explicitly
            tile.label = getString(R.string.tile_label)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_lock_tile)
            
            tile.updateTile()
        }
    }
}