package com.kvl.cyclotrack.vmix

import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.atan2
import kotlin.math.roundToInt

class FrameEngine(private val hub: LiveDataHub, private val frameIntervalMs: Long) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "vmix-frame-engine").apply { isDaemon = true }
    }
    private var future: ScheduledFuture<*>? = null
    private val frameId = AtomicLong(0)
    private val lastFrameRef = AtomicReference<MeasurementFrame?>(null)
    private var lastFrameTimeMs = 0L
    private val fpsFilter = EmaFilter(0.2)
    private val dqFilter = EmaFilter(0.2)
    private val gradeFilter = EmaFilter(0.15)
    private var lastGradeDistanceM: Double? = null
    private var lastGradeAltitudeM: Double? = null
    private val history = ArrayDeque<MeasurementFrame>()
    private val historyLock = Any()
    private val historyMaxMs = 120_000L

    var onFrame: ((MeasurementFrame) -> Unit)? = null

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        future = executor.scheduleAtFixedRate(
            {
                try {
                    val frame = buildFrame()
                    lastFrameRef.set(frame)
                    onFrame?.invoke(frame)
                } catch (_: Throwable) {
                    // Keep scheduler alive even if a frame build fails.
                }
            },
            0L,
            frameIntervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }
        future?.cancel(false)
        future = null
    }

    fun shutdown() {
        stop()
        executor.shutdownNow()
    }

    fun lastFrame(): MeasurementFrame? = lastFrameRef.get()

    fun snapshot(): MeasurementFrame = buildFrame()

    fun history(seconds: Int): List<MeasurementFrame> {
        val cutoff = System.currentTimeMillis() - seconds.coerceAtLeast(0) * 1000L
        synchronized(historyLock) {
            return history.filter { it.timestampMs >= cutoff }
        }
    }

    private fun buildFrame(): MeasurementFrame {
        val now = System.currentTimeMillis()
        val frameId = frameId.incrementAndGet()
        val fps = calculateFps(now)

        val gpsSpeed = hub.lastGpsSpeed
        val bleSpeed = hub.lastBleSpeed
        val gpsSpeedAge = ageMs(gpsSpeed?.timestampMs, now)
        val bleSpeedAge = ageMs(bleSpeed?.timestampMs, now)
        val hasGps = gpsSpeed != null
        val hasBle = bleSpeed != null

        val useGps = when {
            hasGps && !hasBle -> true
            !hasGps && hasBle -> false
            hasGps && hasBle -> gpsSpeedAge <= bleSpeedAge
            else -> false
        }
        val speedSource = when {
            useGps && hasGps -> "gps"
            !useGps && hasBle -> "ble"
            else -> "none"
        }
        val speedGps = gpsSpeed?.value
        val speedBle = bleSpeed?.value
        val speed = when (speedSource) {
            "gps" -> speedGps ?: 0.0
            "ble" -> speedBle ?: 0.0
            else -> 0.0
        }
        val speedSourceAgeMs = when (speedSource) {
            "gps" -> gpsSpeedAge
            "ble" -> bleSpeedAge
            else -> -1L
        }
        val speedConfidence = confidence(speedSourceAgeMs)

        val location = hub.lastLocation
        val lat = location?.value?.first
        val lon = location?.value?.second
        val gpsAgeMs = ageMs(location?.timestampMs, now)
        val gpsConfidence = confidence(gpsAgeMs)

        val altitude = hub.lastAltitudeM
        val altitudeM = altitude?.value
        val altitudeAgeMs = ageMs(altitude?.timestampMs, now)
        val altitudeConfidence = confidence(altitudeAgeMs)

        val distance = hub.lastDistanceM
        val distanceM = distance?.value
        val distanceAgeMs = ageMs(distance?.timestampMs, now)

        val cadence = hub.lastCadence
        val cadenceValue = cadence?.value

        val heartRate = hub.lastHeartRate
        val heartRateValue = heartRate?.value
        val hrAgeMs = ageMs(heartRate?.timestampMs, now)
        val hrConfidence = confidence(hrAgeMs)

        val power = hub.lastPower
        val powerValue = power?.value

        val gradeDeg = computeGradeDeg(
            distanceM = distanceM,
            distanceAgeMs = distanceAgeMs,
            altitudeM = altitudeM,
            altitudeAgeMs = altitudeAgeMs
        )

        val dqRaw = computeDqRaw(speedConfidence, gpsConfidence, hrConfidence, altitudeConfidence)
        val dqSmooth = dqFilter.update(dqRaw.toDouble()).roundToInt()
        val dqReason = computeDqReason(speedConfidence, gpsConfidence, hrConfidence, altitudeConfidence)
        val dqReasonShort = computeDqReasonShort(dqReason)
        val dqState = computeDqState(dqRaw)

        val frame = MeasurementFrame(
            timestampMs = now,
            frameId = frameId,
            fps = fps,
            speed = speed,
            speedGps = speedGps,
            speedBle = speedBle,
            speedSource = speedSource,
            speedSourceAgeMs = speedSourceAgeMs,
            speedConfidence = speedConfidence,
            lat = lat,
            lon = lon,
            gpsAgeMs = gpsAgeMs,
            gpsConfidence = gpsConfidence,
            altitudeM = altitudeM,
            altitudeAgeMs = altitudeAgeMs,
            altitudeConfidence = altitudeConfidence,
            gradeDeg = gradeDeg,
            distanceM = distanceM,
            cadence = cadenceValue,
            heartRate = heartRateValue,
            hrAgeMs = hrAgeMs,
            hrConfidence = hrConfidence,
            power = powerValue,
            powerCal = 0,
            dqRaw = dqRaw,
            dqSmooth = dqSmooth,
            dqState = dqState,
            dqReason = dqReason,
            dqReasonShort = dqReasonShort
        )

        synchronized(historyLock) {
            history.addLast(frame)
            val cutoff = now - historyMaxMs
            while (history.isNotEmpty() && history.first().timestampMs < cutoff) {
                history.removeFirst()
            }
        }

        return frame
    }

    private fun calculateFps(now: Long): Double {
        val last = lastFrameTimeMs
        lastFrameTimeMs = now
        if (last <= 0L) {
            return 1000.0 / max(1L, frameIntervalMs)
        }
        val delta = max(1L, now - last)
        val instantFps = 1000.0 / delta.toDouble()
        return fpsFilter.update(instantFps)
    }

    private fun ageMs(timestampMs: Long?, now: Long): Long {
        if (timestampMs == null) return -1L
        return max(0L, now - timestampMs)
    }

    private fun confidence(ageMs: Long): Int {
        if (ageMs < 0) return 0
        return when {
            ageMs <= 1000L -> 100
            ageMs <= 3000L -> 75
            ageMs <= 5000L -> 50
            ageMs <= 10_000L -> 25
            else -> 0
        }.coerceIn(0, 100)
    }

    private fun computeGradeDeg(
        distanceM: Double?,
        distanceAgeMs: Long,
        altitudeM: Double?,
        altitudeAgeMs: Long
    ): Double {
        if (distanceM == null || altitudeM == null) {
            return 0.0
        }
        if (distanceAgeMs < 0 || altitudeAgeMs < 0) {
            return 0.0
        }
        if (distanceAgeMs > 10_000L || altitudeAgeMs > 10_000L) {
            return 0.0
        }
        val lastDistance = lastGradeDistanceM
        val lastAltitude = lastGradeAltitudeM
        lastGradeDistanceM = distanceM
        lastGradeAltitudeM = altitudeM
        if (lastDistance == null || lastAltitude == null) {
            return 0.0
        }
        val deltaDistance = distanceM - lastDistance
        val deltaAltitude = altitudeM - lastAltitude
        if (deltaDistance <= 0.5) {
            return gradeFilter.update(0.0)
        }
        val gradeRad = atan2(deltaAltitude, deltaDistance)
        val gradeDeg = gradeRad * 180.0 / Math.PI
        return gradeFilter.update(gradeDeg)
    }

    private fun computeDqRaw(
        speedConfidence: Int,
        gpsConfidence: Int,
        hrConfidence: Int,
        altitudeConfidence: Int
    ): Int {
        val weighted = listOf(
            speedConfidence to 4,
            gpsConfidence to 3,
            hrConfidence to 2,
            altitudeConfidence to 1
        )
        val totalWeight = weighted.sumOf { it.second }
        val sum = weighted.sumOf { it.first * it.second }
        return if (totalWeight == 0) 0 else (sum / totalWeight)
    }

    private fun computeDqReason(
        speedConfidence: Int,
        gpsConfidence: Int,
        hrConfidence: Int,
        altitudeConfidence: Int
    ): String {
        return when {
            speedConfidence == 0 && gpsConfidence == 0 -> "no_speed_or_gps"
            speedConfidence == 0 -> "no_speed"
            gpsConfidence == 0 -> "no_gps"
            hrConfidence == 0 -> "no_hr"
            altitudeConfidence == 0 -> "no_altitude"
            speedConfidence < 50 -> "stale_speed"
            gpsConfidence < 50 -> "stale_gps"
            hrConfidence < 50 -> "stale_hr"
            altitudeConfidence < 50 -> "stale_altitude"
            else -> "fresh"
        }
    }

    private fun computeDqReasonShort(reason: String): String {
        return when (reason) {
            "no_speed_or_gps" -> "NO_SPEED_GPS"
            "no_speed" -> "NO_SPEED"
            "no_gps" -> "NO_GPS"
            "no_hr" -> "NO_HR"
            "no_altitude" -> "NO_ALT"
            "stale_speed" -> "STALE_SPEED"
            "stale_gps" -> "STALE_GPS"
            "stale_hr" -> "STALE_HR"
            "stale_altitude" -> "STALE_ALT"
            else -> "OK"
        }
    }

    private fun computeDqState(dqRaw: Int): String {
        return when {
            dqRaw >= 80 -> "good"
            dqRaw >= 50 -> "ok"
            dqRaw >= 20 -> "stale"
            else -> "none"
        }
    }
}
