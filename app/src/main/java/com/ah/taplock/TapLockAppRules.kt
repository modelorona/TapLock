package com.ah.taplock

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale

/** A launchable app shown in the exclusion picker: [packageName] plus its display [label]. */
data class TapLockAppInfo(
    val packageName: String,
    val label: String
)

/**
 * Per-app exclusion rules. The accessibility service records the last foreground package here so
 * rootless entry points (widget, tile) can honor exclusions even without a live service instance.
 */
object TapLockAppRules {
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val ANDROID_PACKAGE = "android"

    /** Shared preferences file used by the whole app. */
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.shared_pref_name), Context.MODE_PRIVATE)

    /** Packages the user excluded from tap-to-lock. */
    fun getExcludedPackages(context: Context): Set<String> {
        val key = context.getString(R.string.excluded_apps)
        return prefs(context).getStringSet(key, emptySet())?.toSet() ?: emptySet()
    }

    /** Replaces the stored exclusion set with [packages]. */
    fun setExcludedPackages(context: Context, packages: Set<String>) {
        val key = context.getString(R.string.excluded_apps)
        prefs(context).edit {
            putStringSet(key, packages.toSet())
        }
    }

    /** Adds [packageName] to the exclusion set, or removes it if already present. */
    fun toggleExcludedPackage(context: Context, packageName: String) {
        val updated = getExcludedPackages(context).toMutableSet().apply {
            if (!add(packageName)) {
                remove(packageName)
            }
        }
        setExcludedPackages(context, updated)
    }

    /**
     * Persists [packageName] as the last real foreground app, skipping the write when it is not
     * trackable or unchanged (SharedPreferences writes are synchronous and this runs per event).
     */
    fun updateForegroundPackage(context: Context, packageName: String?) {
        val trackedPackage = sanitizeTrackedPackage(context, packageName) ?: return

        val key = context.getString(R.string.last_foreground_package)
        val sharedPrefs = prefs(context)
        if (sharedPrefs.getString(key, null) == trackedPackage) return

        sharedPrefs.edit {
            putString(key, trackedPackage)
        }
    }

    /**
     * Filters [packageName] down to something worth tracking: drops blanks, TapLock itself, and
     * system UI overlays that would otherwise mask the app actually behind them.
     */
    fun sanitizeTrackedPackage(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        if (packageName == context.packageName) return null
        if (packageName == SYSTEM_UI_PACKAGE || packageName == ANDROID_PACKAGE) return null
        return packageName
    }

    /** Last tracked foreground package, or null when nothing has been recorded yet. */
    fun getForegroundPackage(context: Context): String? =
        prefs(context).getString(context.getString(R.string.last_foreground_package), null)

    /** True when the last tracked foreground app is in the exclusion set. */
    fun isCurrentAppExcluded(context: Context): Boolean =
        isPackageExcluded(context, getForegroundPackage(context))

    /** True when [packageName] is non-blank and in the exclusion set. */
    fun isPackageExcluded(context: Context, packageName: String?): Boolean =
        !packageName.isNullOrBlank() && getExcludedPackages(context).contains(packageName)

    /**
     * Loads all launcher-visible apps (excluding TapLock) as [TapLockAppInfo], deduplicated and
     * sorted case-insensitively by label for the exclusion picker.
     */
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

    /** Human-readable label for [packageName], falling back to the package name itself. */
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
