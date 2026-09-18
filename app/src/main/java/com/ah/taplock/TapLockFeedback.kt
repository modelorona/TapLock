package com.ah.taplock

import android.app.StatusBarManager
import android.content.Context
import android.widget.Toast

/** Centralized user-facing toasts so widget, tile, and service surfaces show identical wording. */
object TapLockFeedback {
    /** Tells the user the lock was skipped because the foreground app is excluded. */
    fun showAppExcluded(context: Context) {
        toast(context, R.string.app_exclusions_disabled_toast)
    }

    /** Tells the user the accessibility service must be enabled before locking can work. */
    fun showAccessibilityRequired(context: Context) {
        toast(context, R.string.accessibility_permission_required, Toast.LENGTH_LONG)
    }

    /** Tells the user the root shell rejected the lock command (e.g. permission revoked). */
    fun showRootLockFailed(context: Context) {
        toast(context, R.string.root_lock_failed, Toast.LENGTH_LONG)
    }

    /** Tells the user the launcher does not support programmatic widget pinning. */
    fun showWidgetPinUnsupported(context: Context) {
        toast(context, R.string.widget_pin_not_supported_toast, Toast.LENGTH_LONG)
    }

    /** Tells the user the tile must be added manually (no StatusBarManager request support). */
    fun showQuickSettingsAddUnsupported(context: Context) {
        toast(context, R.string.quick_settings_tile_manual_toast, Toast.LENGTH_LONG)
    }

    /** Maps a StatusBarManager tile-add [result] code to the matching toast. */
    fun showQuickSettingsAddResult(context: Context, result: Int) {
        when (result) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                toast(context, R.string.quick_settings_tile_added_toast)
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                toast(context, R.string.quick_settings_tile_already_added_toast)
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                toast(context, R.string.quick_settings_tile_not_added_toast)
            else -> toast(context, R.string.quick_settings_tile_failed_toast, Toast.LENGTH_LONG)
        }
    }

    /** Shows [messageRes] as a toast for [duration]. */
    private fun toast(
        context: Context,
        messageRes: Int,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(context, context.getString(messageRes), duration).show()
    }
}
