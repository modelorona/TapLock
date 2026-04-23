package com.ah.taplock

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale

data class TapLockAppInfo(
    val packageName: String,
    val label: String
)

object TapLockAppRules {
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val ANDROID_PACKAGE = "android"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.shared_pref_name), Context.MODE_PRIVATE)

    fun getExcludedPackages(context: Context): Set<String> {
        val key = context.getString(R.string.excluded_apps)
        return prefs(context).getStringSet(key, emptySet())?.toSet() ?: emptySet()
    }

    fun setExcludedPackages(context: Context, packages: Set<String>) {
        val key = context.getString(R.string.excluded_apps)
        prefs(context).edit {
            putStringSet(key, packages.toSet())
        }
    }

    fun toggleExcludedPackage(context: Context, packageName: String) {
        val updated = getExcludedPackages(context).toMutableSet().apply {
            if (!add(packageName)) {
                remove(packageName)
            }
        }
        setExcludedPackages(context, updated)
    }

    fun updateForegroundPackage(context: Context, packageName: String?) {
        val trackedPackage = sanitizeTrackedPackage(context, packageName) ?: return

        val key = context.getString(R.string.last_foreground_package)
        val sharedPrefs = prefs(context)
        if (sharedPrefs.getString(key, null) == trackedPackage) return

        sharedPrefs.edit {
            putString(key, trackedPackage)
        }
    }

    fun sanitizeTrackedPackage(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        if (packageName == context.packageName) return null
        if (packageName == SYSTEM_UI_PACKAGE || packageName == ANDROID_PACKAGE) return null
        return packageName
    }

    fun getForegroundPackage(context: Context): String? =
        prefs(context).getString(context.getString(R.string.last_foreground_package), null)

    fun isCurrentAppExcluded(context: Context): Boolean =
        isPackageExcluded(context, getForegroundPackage(context))

    fun isPackageExcluded(context: Context, packageName: String?): Boolean =
        !packageName.isNullOrBlank() && getExcludedPackages(context).contains(packageName)

    fun loadLaunchableApps(context: Context): List<TapLockAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        @Suppress("DEPRECATION")
        val resolvedApps = packageManager.queryIntentActivities(intent, 0)

        return resolvedApps
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null

                TapLockAppInfo(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(packageManager).toString().trim()
                        .takeIf { it.isNotEmpty() }
                        ?: packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<TapLockAppInfo> { it.label.lowercase(Locale.getDefault()) }
                    .thenBy { it.packageName }
            )
    }

    fun resolveAppLabel(context: Context, packageName: String): String {
        val packageManager = context.packageManager
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString().trim()
                .takeIf { it.isNotEmpty() }
                ?: packageName
        } catch (_: Exception) {
            packageName
        }
    }
}
