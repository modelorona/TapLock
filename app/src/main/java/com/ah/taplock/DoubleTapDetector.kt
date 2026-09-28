package com.ah.taplock

/**
 * Time-window double-tap detector. [currentTimeMs] is injectable so tests can drive the clock
 * deterministically instead of relying on real time.
 */
class DoubleTapDetector(
    private val currentTimeMs: () -> Long = System::currentTimeMillis
) {
    private var lastTapTime = 0L

    /**
     * Records a tap at [tapTimeMs] and returns true when it lands within [timeoutMs] of the
     * previous one. A detected double tap consumes both taps, so a third tap starts a new window.
     */
    fun onTap(timeoutMs: Int, tapTimeMs: Long = currentTimeMs()): Boolean {
        if (lastTapTime != 0L && tapTimeMs - lastTapTime < timeoutMs) {
            lastTapTime = 0L
            return true
        }
        lastTapTime = tapTimeMs
        return false
    }

    /** Forgets the pending tap, e.g. when the tap target moves or is resized mid-sequence. */
    fun reset() {
        lastTapTime = 0L
    }
}
