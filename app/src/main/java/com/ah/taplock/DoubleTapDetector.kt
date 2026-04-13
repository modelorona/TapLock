package com.ah.taplock

class DoubleTapDetector(
    private val currentTimeMs: () -> Long = System::currentTimeMillis
) {
    private var lastTapTime = 0L

    fun onTap(timeoutMs: Int, tapTimeMs: Long = currentTimeMs()): Boolean {
        if (lastTapTime != 0L && tapTimeMs - lastTapTime < timeoutMs) {
            lastTapTime = 0L
            return true
        }
        lastTapTime = tapTimeMs
        return false
    }

    fun reset() {
        lastTapTime = 0L
    }
}
