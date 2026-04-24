package com.ah.taplock

import android.app.StatusBarManager
import android.content.Context
import android.widget.Toast

object TapLockFeedback {
    fun showAppExcluded(context: Context) {
        toast(context, R.string.app_exclusions_disabled_toast)
    }

    fun showAccessibilityRequired(context: Context) {
        toast(context, R.string.accessibility_permission_required, Toast.LENGTH_LONG)
    }

    fun showOverlayPermissionRequired(context: Context) {
        toast(context, R.string.overlay_permission_required_toast)
    }

    fun showWidgetPinUnsupported(context: Context) {
        toast(context, R.string.widget_pin_not_supported_toast, Toast.LENGTH_LONG)
    }

    fun showQuickSettingsAddUnsupported(context: Context) {
        toast(context, R.string.quick_settings_tile_manual_toast, Toast.LENGTH_LONG)
    }

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

    private fun toast(
        context: Context,
        messageRes: Int,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(context, context.getString(messageRes), duration).show()
    }
}
