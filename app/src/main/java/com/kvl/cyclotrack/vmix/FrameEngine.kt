package com.kvl.cyclotrack.vmix

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class FrameEngine(
    private val hub: LiveDataHub,
    private val frameIntervalMs: Long
) {
    private val lock = Any()
    private val historyLock = Any()
    private val gson: Gson = GsonBuilder().serializeNulls().create()

    private var executor: ScheduledExecutorService? = null
    private var running = false

    private var frameId = 0L
    private var lastFrameTimeMs = 0L

    private val fpsEma = EmaFilter(0.25)
    private val speedEma = EmaFilter(0.30)
    private val speedGpsEma = EmaFilter(0.25)
    private val altitudeEma = EmaFilter(0.10)
    private val dqEma = EmaFilter(0.20)

    private var dqState: String = DATA_QUALITY_NODATA
    private var lastAltForGrade: Double? = null
    private var lastDistanceForGrade: Double? = null
    private var internalDistanceM = 0.0

    private val history = ArrayDeque<MeasurementFrame>(HISTORY_MAX_FRAMES)

    @Volatile private var lastFrame: MeasurementFrame? = null
    @Volatile private var lastVmixJsonString: String = "[]"

    fun start() {
        synchronized(lock) {
            if (running) return
            resetState()
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            executor = scheduler
            running = true
            scheduler.scheduleAtFixedRate(
                { tickSafe() },
                0L,
                frameIntervalMs,
                TimeUnit.MILLISECONDS
            )
        }
    }

    fun stop() {
        synchronized(lock) {
            if (!running) return
            running = false
            executor?.shutdownNow()
            executor = null
        }
    }

    fun getLastVmixSnapshotJson(): String = lastVmixJsonString

    fun getHistoryJson(seconds: Int): String {
        val clampedSeconds = seconds.coerceIn(1, HISTORY_SECONDS)
        val maxFrames = ((clampedSeconds * 1000L) / frameIntervalMs).toInt().coerceAtLeast(1)
        val frames = synchronized(historyLock) {
            if (history.isEmpty()) {
                emptyList()
            } else {
                history.toList().takeLast(maxFrames)
            }
        }
        val historyFrames = frames.map {
            HistoryFrame(
                ts = it.timestampMs,
                speed = it.speed,
                hr = it.heartRate,
                dq = it.dqSmooth,
                grade = it.gradeDeg,
                lat = it.lat,
                lon = it.lon
            )
        }
        return gson.toJson(HistoryResponse(clampedSeconds, historyFrames))
    }

    private fun resetState() {
        frameId = 0L
        lastFrameTimeMs = 0L
        fpsEma.reset()
        speedEma.reset()
        speedGpsEma.reset()
        altitudeEma.reset()
        dqEma.reset()
        dqState = DATA_QUALITY_NODATA
        lastAltForGrade = null
        lastDistanceForGrade = null
        internalDistanceM = 0.0
        synchronized(historyLock) {
            history.clear()
        }
        lastFrame = null
        lastVmixJsonString = "[]"
    }

    private fun tickSafe() {
        try {
            tick()
        } catch (_: Throwable) {
            // Keep engine alive even if something goes wrong in a single frame.
        }
    }

    private fun tick() {
        val nowMs = System.currentTimeMillis()
        val dtMs = if (lastFrameTimeMs > 0L) nowMs - lastFrameTimeMs else 0L
        lastFrameTimeMs = nowMs
        val dtSeconds = if (dtMs > 0L) dtMs / 1000.0 else 0.0

        val fpsRaw = if (dtMs > 0L) 1000.0 / dtMs else 0.0
        val fps = fpsEma.update(fpsRaw)

        val lastGpsSpeed = hub.lastGpsSpeed
        val lastBleSpeed = hub.lastBleSpeed
        val lastLocation = hub.lastLocation
        val lastAltitude = hub.lastAltitudeM
        val lastDistance = hub.lastDistanceM
        val lastHeartRate = hub.lastHeartRate
        val lastCadence = hub.lastCadence
        val lastPower = hub.lastPower

        val gpsAgeMs = ageMs(nowMs, lastLocation)
        val gpsConfidence = confidencePct(gpsAgeMs, GPS_MAX_AGE_MS)

        val altitudeAgeMs = ageMs(nowMs, lastAltitude)
        val altitudeConfidence = confidencePct(altitudeAgeMs, ALT_MAX_AGE_MS)

        val hrAgeMs = ageMs(nowMs, lastHeartRate)
        val hrConfidence = confidencePct(hrAgeMs, HR_MAX_AGE_MS)

        val bleAgeMs = ageMs(nowMs, lastBleSpeed)
        val gpsSpeedAgeMs = ageMs(nowMs, lastGpsSpeed)

        val speedSource = when {
            lastBleSpeed != null && bleAgeMs <= SPEED_BLE_MAX_AGE_MS -> SPEED_SOURCE_BLE
            lastGpsSpeed != null && gpsSpeedAgeMs <= SPEED_GPS_MAX_AGE_MS -> SPEED_SOURCE_GPS
            else -> SPEED_SOURCE_NONE
        }

        val speedSourceAgeMs = when (speedSource) {
            SPEED_SOURCE_BLE -> bleAgeMs
            SPEED_SOURCE_GPS -> gpsSpeedAgeMs
            else -> -1L
        }

        val speedSourceMaxAge = when (speedSource) {
            SPEED_SOURCE_BLE -> SPEED_BLE_MAX_AGE_MS
            SPEED_SOURCE_GPS -> SPEED_GPS_MAX_AGE_MS
            else -> 0L
        }

        val speedRaw = when (speedSource) {
            SPEED_SOURCE_BLE -> lastBleSpeed?.value ?: 0.0
            SPEED_SOURCE_GPS -> lastGpsSpeed?.value ?: 0.0
            else -> 0.0
        }
        val speed = speedEma.update(speedRaw)

        val speedGps = lastGpsSpeed?.value?.let { speedGpsEma.update(it) }
        val speedBle = lastBleSpeed?.value

        val speedConfidence = if (speedSource == SPEED_SOURCE_NONE) {
            0
        } else {
            confidencePct(speedSourceAgeMs, speedSourceMaxAge)
        }

        val lat = lastLocation?.value?.first
        val lon = lastLocation?.value?.second

        val altitudeSmoothed = if (lastAltitude != null) {
            altitudeEma.update(lastAltitude.value)
        } else {
            altitudeEma.get()
        }

        val cadence = lastCadence?.value
        val heartRate = lastHeartRate?.value
        val power = lastPower?.value

        val distanceM = lastDistance?.value
        val speedMps = speed / 3.6
        val deltaDistM = if (distanceM != null) {
            val prev = lastDistanceForGrade
            lastDistanceForGrade = distanceM
            internalDistanceM = distanceM
            if (prev == null) 0.0 else distanceM - prev
        } else {
            val delta = if (dtSeconds > 0.0) speedMps * dtSeconds else 0.0
            internalDistanceM += delta
            lastDistanceForGrade = internalDistanceM
            delta
        }

        val gradeDeg = if (deltaDistM >= 1.0 && altitudeSmoothed != null && lastAltForGrade != null) {
            Math.toDegrees(atan((altitudeSmoothed - (lastAltForGrade ?: altitudeSmoothed)) / deltaDistM))
        } else {
            0.0
        }
        if (altitudeSmoothed != null) {
            lastAltForGrade = altitudeSmoothed
        }

        val powerCal = calculatePowerCal(speedMps, gradeDeg, hub.riderKg + hub.bikeKg)

        var dqRaw = (0.45 * speedConfidence + 0.25 * gpsConfidence + 0.20 * altitudeConfidence + 0.10 * hrConfidence)
            .roundToInt()
        dqRaw = when {
            speedConfidence < 10 -> min(dqRaw, 10)
            speedConfidence < 30 -> min(dqRaw, 30)
            else -> dqRaw
        }.coerceIn(0, 100)

        val dqSmooth = dqEma.update(dqRaw.toDouble()).roundToInt().coerceIn(0, 100)
        dqState = updateDqState(dqState, dqSmooth)

        val dqReason = buildDqReason(
            dqState = dqState,
            speedSource = speedSource,
            speedSourceAgeMs = speedSourceAgeMs,
            speedSourceMaxAgeMs = speedSourceMaxAge,
            speedConfidence = speedConfidence,
            gpsAgeMs = gpsAgeMs,
            gpsConfidence = gpsConfidence,
            altitudeAgeMs = altitudeAgeMs,
            altitudeConfidence = altitudeConfidence,
            hrAgeMs = hrAgeMs,
            hrConfidence = hrConfidence
        )

        val dqReasonShort = buildDqReasonShort(dqState, dqReason)

        val frame = MeasurementFrame(
            timestampMs = nowMs,
            frameId = frameId++,
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
            altitudeM = altitudeSmoothed,
            altitudeAgeMs = altitudeAgeMs,
            altitudeConfidence = altitudeConfidence,
            gradeDeg = gradeDeg,
            distanceM = distanceM,
            cadence = cadence,
            heartRate = heartRate,
            hrAgeMs = hrAgeMs,
            hrConfidence = hrConfidence,
            power = power,
            powerCal = powerCal,
            dqRaw = dqRaw,
            dqSmooth = dqSmooth,
            dqState = dqState,
            dqReason = dqReason,
            dqReasonShort = dqReasonShort
        )

        synchronized(historyLock) {
            history.addLast(frame)
            while (history.size > HISTORY_MAX_FRAMES) {
                history.removeFirst()
            }
        }

        lastFrame = frame
        lastVmixJsonString = VmixMapper.toVmixJson(frame)
    }

    private fun calculatePowerCal(speedMps: Double, gradeDeg: Double, totalMassKg: Double): Int {
        if (speedMps <= 0.0 || totalMassKg <= 0.0) return 0
        val rho = 1.226
        val cdA = 0.32
        val crr = 0.005
        val g = 9.80665
        val gradeRad = Math.toRadians(gradeDeg)

        val paero = 0.5 * rho * cdA * speedMps * speedMps * speedMps
        val proll = crr * totalMassKg * g * speedMps
        val pgrav = totalMassKg * g * sin(gradeRad) * speedMps
        return max(0.0, paero + proll + pgrav).roundToInt()
    }

    private fun updateDqState(current: String, dqSmooth: Int): String = when (current) {
        DATA_QUALITY_OK -> if (dqSmooth <= 75) DATA_QUALITY_WARN else DATA_QUALITY_OK
        DATA_QUALITY_WARN -> when {
            dqSmooth >= 85 -> DATA_QUALITY_OK
            dqSmooth <= 55 -> DATA_QUALITY_BAD
            else -> DATA_QUALITY_WARN
        }
        DATA_QUALITY_BAD -> when {
            dqSmooth >= 65 -> DATA_QUALITY_WARN
            dqSmooth <= 10 -> DATA_QUALITY_NODATA
            else -> DATA_QUALITY_BAD
        }
        DATA_QUALITY_NODATA -> if (dqSmooth >= 20) DATA_QUALITY_BAD else DATA_QUALITY_NODATA
        else -> DATA_QUALITY_NODATA
    }

    private fun buildDqReason(
        dqState: String,
        speedSource: String,
        speedSourceAgeMs: Long,
        speedSourceMaxAgeMs: Long,
        speedConfidence: Int,
        gpsAgeMs: Long,
        gpsConfidence: Int,
        altitudeAgeMs: Long,
        altitudeConfidence: Int,
        hrAgeMs: Long,
        hrConfidence: Int
    ): String {
        if (dqState == DATA_QUALITY_NODATA) return DATA_QUALITY_NODATA

        val reasons = mutableListOf<String>()

        if (speedSource == SPEED_SOURCE_NONE) reasons.add("SPEED_NONE")
        if (speedSource != SPEED_SOURCE_NONE && speedSourceAgeMs > speedSourceMaxAgeMs) reasons.add("SPEED_STALE")
        if (speedConfidence < 30) reasons.add("SPEED_LOW_CONF")

        if (gpsAgeMs < 0L) reasons.add("GPS_NONE")
        if (gpsAgeMs > GPS_MAX_AGE_MS) reasons.add("GPS_STALE")
        if (gpsConfidence < 30) reasons.add("GPS_LOW_CONF")

        if (altitudeAgeMs < 0L) reasons.add("ALT_NONE")
        if (altitudeAgeMs > ALT_MAX_AGE_MS) reasons.add("ALT_STALE")
        if (altitudeConfidence < 30) reasons.add("ALT_LOW_CONF")

        if (hrAgeMs < 0L) reasons.add("HR_NONE")
        if (hrAgeMs > HR_MAX_AGE_MS) reasons.add("HR_STALE")
        if (hrConfidence < 30) reasons.add("HR_LOW_CONF")

        return if (reasons.isEmpty()) "OK" else reasons.joinToString("|")
    }

    private fun buildDqReasonShort(dqState: String, dqReason: String): String {
        if (dqState == DATA_QUALITY_NODATA) return DATA_QUALITY_NODATA
        val parts = dqReason.split("|")
        return when {
            parts.any { it.startsWith("SPEED_") } -> "SPEED"
            parts.any { it.startsWith("GPS_") } -> "GPS"
            parts.any { it.startsWith("ALT_") } -> "ALT"
            parts.any { it.startsWith("HR_") } -> "HR"
            else -> "OK"
        }
    }

    private fun <T> ageMs(nowMs: Long, timedValue: TimedValue<T>?): Long {
        return if (timedValue == null) -1L else nowMs - timedValue.timestampMs
    }

    private fun confidencePct(ageMs: Long, maxAgeMs: Long): Int {
        if (ageMs < 0L || maxAgeMs <= 0L) return 0
        val ratio = 1.0 - (ageMs.toDouble() / maxAgeMs.toDouble())
        return (ratio * 100.0).roundToInt().coerceIn(0, 100)
    }

    private data class HistoryFrame(
        val ts: Long,
        val speed: Double,
        val hr: Int?,
        val dq: Int,
        val grade: Double,
        val lat: Double?,
        val lon: Double?
    )

    private data class HistoryResponse(
        val seconds: Int,
        val frames: List<HistoryFrame>
    )

    companion object {
        private const val HISTORY_SECONDS = 60
        private const val HISTORY_MAX_FRAMES = 120

        private const val SPEED_BLE_MAX_AGE_MS = 1500L
        private const val SPEED_GPS_MAX_AGE_MS = 2500L
        private const val GPS_MAX_AGE_MS = 2500L
        private const val ALT_MAX_AGE_MS = 2500L
        private const val HR_MAX_AGE_MS = 3000L

        private const val SPEED_SOURCE_BLE = "BLE"
        private const val SPEED_SOURCE_GPS = "GPS"
        private const val SPEED_SOURCE_NONE = "NONE"

        private const val DATA_QUALITY_OK = "OK"
        private const val DATA_QUALITY_WARN = "WARN"
        private const val DATA_QUALITY_BAD = "BAD"
        private const val DATA_QUALITY_NODATA = "NODATA"
    }
}
