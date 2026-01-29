package com.kvl.cyclotrack.vmix

class LiveDataHub {
    @Volatile var lastGpsSpeed: TimedValue<Double>? = null
    @Volatile var lastBleSpeed: TimedValue<Double>? = null
    @Volatile var lastLocation: TimedValue<Pair<Double, Double>>? = null
    @Volatile var lastAltitudeM: TimedValue<Double>? = null
    @Volatile var lastDistanceM: TimedValue<Double>? = null
    @Volatile var lastHeartRate: TimedValue<Int>? = null
    @Volatile var lastCadence: TimedValue<Int>? = null
    @Volatile var lastPower: TimedValue<Int>? = null
    @Volatile var riderKg: Double = 70.0
    @Volatile var bikeKg: Double = 10.0

    fun setGps(lat: Double, lon: Double, altitudeM: Double, speedMps: Float, nowMs: Long) {
        lastLocation = TimedValue(lat to lon, nowMs)
        lastAltitudeM = TimedValue(altitudeM, nowMs)
        lastGpsSpeed = TimedValue(speedMps.toDouble() * 3.6, nowMs)
    }

    fun setHeartRate(hrBpm: Int, nowMs: Long) {
        lastHeartRate = TimedValue(hrBpm, nowMs)
    }

    fun setCadence(cadRpm: Int, nowMs: Long) {
        lastCadence = TimedValue(cadRpm, nowMs)
    }

    fun setBleSpeed(speedMps: Double, nowMs: Long) {
        lastBleSpeed = TimedValue(speedMps * 3.6, nowMs)
    }
}
