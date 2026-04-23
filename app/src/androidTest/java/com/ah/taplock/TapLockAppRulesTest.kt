package com.ah.taplock

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TapLockAppRulesTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val sharedPrefName: String
        get() = context.getString(R.string.shared_pref_name)

    private fun getPrefs(): SharedPreferences =
        context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)

    @Before
    fun setup() {
        getPrefs().edit().clear().commit()
    }

    @Test
    fun setExcludedPackages_roundTripsThroughPrefs() {
        val packages = setOf("com.aegis.launcher", "com.example.notes")

        TapLockAppRules.setExcludedPackages(context, packages)

        assertEquals(packages, TapLockAppRules.getExcludedPackages(context))
    }

    @Test
    fun toggleExcludedPackage_addsAndRemovesSamePackage() {
        val packageName = "com.aegis.launcher"

        TapLockAppRules.toggleExcludedPackage(context, packageName)
        assertTrue(TapLockAppRules.getExcludedPackages(context).contains(packageName))

        TapLockAppRules.toggleExcludedPackage(context, packageName)
        assertFalse(TapLockAppRules.getExcludedPackages(context).contains(packageName))
    }

    @Test
    fun updateForegroundPackage_ignoresNonTrackablePackages() {
        val lastForegroundKey = context.getString(R.string.last_foreground_package)
        getPrefs().edit().putString(lastForegroundKey, "com.example.keep").commit()

        TapLockAppRules.updateForegroundPackage(context, null)
        TapLockAppRules.updateForegroundPackage(context, "")
        TapLockAppRules.updateForegroundPackage(context, context.packageName)
        TapLockAppRules.updateForegroundPackage(context, "android")
        TapLockAppRules.updateForegroundPackage(context, "com.android.systemui")

        assertEquals("com.example.keep", TapLockAppRules.getForegroundPackage(context))
    }

    @Test
    fun isCurrentAppExcluded_matchesTrackedForegroundPackage() {
        TapLockAppRules.setExcludedPackages(
            context,
            setOf("com.aegis.launcher", "com.example.notes")
        )

        TapLockAppRules.updateForegroundPackage(context, "com.aegis.launcher")
        assertTrue(TapLockAppRules.isCurrentAppExcluded(context))

        TapLockAppRules.updateForegroundPackage(context, "com.example.calendar")
        assertFalse(TapLockAppRules.isCurrentAppExcluded(context))
    }

    @Test
    fun sanitizeTrackedPackage_filtersTapLockAndSystemPackages() {
        assertNull(TapLockAppRules.sanitizeTrackedPackage(context, null))
        assertNull(TapLockAppRules.sanitizeTrackedPackage(context, ""))
        assertNull(TapLockAppRules.sanitizeTrackedPackage(context, context.packageName))
        assertNull(TapLockAppRules.sanitizeTrackedPackage(context, "android"))
        assertNull(TapLockAppRules.sanitizeTrackedPackage(context, "com.android.systemui"))
        assertEquals(
            "com.aegis.launcher",
            TapLockAppRules.sanitizeTrackedPackage(context, "com.aegis.launcher")
        )
    }
}
