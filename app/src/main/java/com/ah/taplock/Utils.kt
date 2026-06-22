package com.ah.taplock

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager

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
