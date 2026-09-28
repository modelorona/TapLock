package com.ah.taplock

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Root-based lock implementation. Instead of GLOBAL_ACTION_LOCK_SCREEN (which briefly shows the
 * lock screen before the display turns off, causing a visible flicker), it injects KEYCODE_POWER
 * via a root shell — the exact effect of pressing the power button: the screen turns off
 * immediately with no flicker.
 */
object RootLock {

    private const val TAG = "TapLock"
    const val LOCK_METHOD_ACCESSIBILITY = "ACCESSIBILITY"
    const val LOCK_METHOD_ROOT = "ROOT"
    private const val KEYCODE_POWER_COMMAND = "input keyevent 26"

    // Reused across locks so each lock only pays for the `input` call, not a new su session.
    private var shell: Process? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Cheap heuristic check for a rooted device: looks for a `su` binary on disk and in PATH.
     * Note: Magisk-style managers mount `su` into an app's namespace only at process start, so a
     * process launched before root was available won't see it until it is restarted (see README).
     */
    fun isDeviceRooted(): Boolean {
        val commonPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/system/sbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/vendor/bin/su",
            "/odm/bin/su",
            "/product/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        if (commonPaths.any { runCatching { File(it).exists() }.getOrDefault(false) }) return true
        val pathDirs = System.getenv("PATH")?.split(':').orEmpty()
        if (pathDirs.any { dir ->
                dir.isNotEmpty() && runCatching { File(dir, "su").exists() }.getOrDefault(false)
            }
        ) return true
        // Some root setups hide the binary from direct path probes; a shell lookup can still
        // resolve it without triggering a superuser prompt.
        return runCatching {
            val process = ProcessBuilder("which", "su").start()
            val finished = process.waitFor(2, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            finished && process.exitValue() == 0 &&
                process.inputStream.bufferedReader().readText().isNotBlank()
        }.getOrDefault(false)
    }

    /** True when the user selected root as the lock method. */
    fun isRootLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            context.getString(R.string.shared_pref_name),
            Context.MODE_PRIVATE
        )
        return prefs.getString(
            context.getString(R.string.lock_method),
            null
        ) == LOCK_METHOD_ROOT
    }

    /**
     * Runs `su -c id` to trigger the superuser grant prompt and verify access. [onResult] is
     * invoked on the main thread.
     */
    fun verifyRootAccess(onResult: (Boolean) -> Unit) {
        thread(name = "TapLockRootCheck") {
            val granted = runCatching {
                val process = ProcessBuilder("su", "-c", "id").start()
                val output = process.inputStream.bufferedReader().readText()
                // Generous timeout: the superuser manager may wait for the user to respond.
                val finished = process.waitFor(60, TimeUnit.SECONDS)
                if (!finished) process.destroyForcibly()
                finished && process.exitValue() == 0 && output.contains("uid=0")
            }.getOrDefault(false)
            mainHandler.post { onResult(granted) }
        }
    }

    /**
     * Locks the screen by injecting KEYCODE_POWER through the root shell. Increments the lock
     * counter on success. [onResult] is invoked on the main thread; callers can fall back to the
     * accessibility lock when it reports false.
     */
    fun performRootLock(context: Context, onResult: (Boolean) -> Unit = {}) {
        val appContext = context.applicationContext
        thread(name = "TapLockRootLock") {
            val success = runShellCommand(KEYCODE_POWER_COMMAND)
            if (success) {
                val prefs = appContext.getSharedPreferences(
                    appContext.getString(R.string.shared_pref_name),
                    Context.MODE_PRIVATE
                )
                val count = prefs.getInt(appContext.getString(R.string.lock_count), 0)
                prefs.edit { putInt(appContext.getString(R.string.lock_count), count + 1) }
            } else {
                Log.w(TAG, "Root lock failed: could not write to su shell")
            }
            mainHandler.post { onResult(success) }
        }
    }

    @Synchronized
    private fun runShellCommand(command: String): Boolean {
        repeat(2) {
            val process = shell?.takeIf { it.isAlive }
                ?: runCatching { ProcessBuilder("su").start() }
                    .getOrNull()
                    ?.also { shell = it }
                ?: return false
            try {
                process.outputStream.write("$command\n".toByteArray())
                process.outputStream.flush()
                return true
            } catch (e: IOException) {
                // Shell died (e.g. permission revoked); drop it and retry once with a fresh one.
                Log.d(TAG, "Root shell write failed, restarting shell", e)
                runCatching { process.destroy() }
                shell = null
            }
        }
        return false
    }
}
