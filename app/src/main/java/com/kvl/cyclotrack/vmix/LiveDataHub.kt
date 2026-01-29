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
}
