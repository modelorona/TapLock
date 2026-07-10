package com.ah.taplock

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.security.advancedprotection.AdvancedProtectionManager
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

/**
 * Whether Advanced Protection Mode (Android 16+) is enabled on the device. When it is, the system
 * restricts accessibility services to preinstalled/trusted tools and blocks third-party apps like
 * TapLock from being enabled — the toggle in Settings is greyed out with no way to override. There
 * is no way for TapLock to work around this, so callers use it to explain the situation rather than
 * point the user at an accessibility toggle they can't flip.
 */
fun isAdvancedProtectionEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 36) return false
    val manager = context.getSystemService(AdvancedProtectionManager::class.java) ?: return false
    return manager.isAdvancedProtectionEnabled
}

/**
 * Registers [onChanged] to be notified when Advanced Protection Mode is toggled, returning a handle
 * that must be passed to [unregisterAdvancedProtectionCallback] to clean up. Returns null on
 * devices below Android 16 or when the manager is unavailable. The callback fires once on
 * registration with the current state.
 */
@RequiresApi(36)
fun registerAdvancedProtectionCallback(
    context: Context,
    executor: Executor,
    onChanged: (Boolean) -> Unit
): AdvancedProtectionManager.Callback? {
    val manager = context.getSystemService(AdvancedProtectionManager::class.java) ?: return null
    val callback = AdvancedProtectionManager.Callback { enabled -> onChanged(enabled) }
    manager.registerAdvancedProtectionCallback(executor, callback)
    return callback
}

@RequiresApi(36)
fun unregisterAdvancedProtectionCallback(
    context: Context,
    callback: AdvancedProtectionManager.Callback
) {
    val manager = context.getSystemService(AdvancedProtectionManager::class.java) ?: return
    manager.unregisterAdvancedProtectionCallback(callback)
}

fun isAccessibilityEnabled(context: Context): Boolean {
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val serviceComponent = ComponentName(context, TapLockAccessibilityService::class.java)

    return accessibilityManager
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { serviceInfo ->
            val resolveInfo = serviceInfo.resolveInfo.serviceInfo
            ComponentName(resolveInfo.packageName, resolveInfo.name) == serviceComponent
        }
}
