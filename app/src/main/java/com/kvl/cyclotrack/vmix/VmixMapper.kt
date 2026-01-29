package com.kvl.cyclotrack.vmix

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object VmixMapper {
    private val gson: Gson = GsonBuilder().serializeNulls().create()

    fun toVmixJson(frame: MeasurementFrame): String = gson.toJson(listOf(frameToMap(frame)))

    fun toVmixJson(frames: List<MeasurementFrame>): String =
        gson.toJson(frames.map { frameToMap(it) })

    private fun frameToMap(frame: MeasurementFrame): Map<String, Any?> = mapOf(
        "timestampMs" to frame.timestampMs,
        "frameId" to frame.frameId,
        "fps" to frame.fps,
        "speed" to frame.speed,
        "speedGps" to frame.speedGps,
        "speedBle" to frame.speedBle,
        "speedSource" to frame.speedSource,
        "speedSourceAgeMs" to frame.speedSourceAgeMs,
        "speedConfidence" to frame.speedConfidence,
        "lat" to frame.lat,
        "lon" to frame.lon,
        "gpsAgeMs" to frame.gpsAgeMs,
        "gpsConfidence" to frame.gpsConfidence,
        "altitudeM" to frame.altitudeM,
        "altitudeAgeMs" to frame.altitudeAgeMs,
        "altitudeConfidence" to frame.altitudeConfidence,
        "gradeDeg" to frame.gradeDeg,
        "distanceM" to frame.distanceM,
        "cadence" to frame.cadence,
        "heartRate" to frame.heartRate,
        "hrAgeMs" to frame.hrAgeMs,
        "hrConfidence" to frame.hrConfidence,
        "power" to frame.power,
        "powerCal" to frame.powerCal,
        "dqRaw" to frame.dqRaw,
        "dqSmooth" to frame.dqSmooth,
        "dqState" to frame.dqState,
        "dqReason" to frame.dqReason,
        "dqReasonShort" to frame.dqReasonShort,
    )
}
