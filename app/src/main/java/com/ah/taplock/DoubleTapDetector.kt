package com.ah.taplock

class DoubleTapDetector(
    private val currentTimeMs: () -> Long = System::currentTimeMillis
) {
    private var lastTapTime = 0L

    fun onTap(timeoutMs: Int, tapTimeMs: Long = currentTimeMs()): Boolean {
        val currentTime = tapTimeMs
        if (lastTapTime != 0L && currentTime - lastTapTime < timeoutMs) {
            lastTapTime = 0L
            return true
        }
        lastTapTime = currentTime
        return false
    }

    fun reset() {
        lastTapTime = 0L
    }
}
