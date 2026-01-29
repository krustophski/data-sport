package com.kvl.cyclotrack.vmix

class EmaFilter(private val alpha: Double) {
    private var initialized = false
    private var value = 0.0

    fun update(input: Double): Double {
        value = if (!initialized) {
            initialized = true
            input
        } else {
            alpha * input + (1 - alpha) * value
        }
        return value
    }
}
