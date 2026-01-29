package com.kvl.cyclotrack.vmix

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.Locale

object VmixMapper {
    private val gson: Gson = GsonBuilder().serializeNulls().create()

    fun toVmixJson(frame: MeasurementFrame): String = gson.toJson(frameToParams(frame))

    private fun frameToParams(frame: MeasurementFrame): List<VmixParam> {
        val phoneTime = formatFixed(frame.timestampMs / 1000.0, 3)
        val fps = formatFixed(frame.fps, 2)
        val speed = formatFixedTrim(frame.speed, 2)
        val speedGps = formatFixedTrim(frame.speedGps ?: 0.0, 2)
        val speedBle = formatFixedTrim(frame.speedBle ?: 0.0, 2)
        val cadence = (frame.cadence ?: 0).toString()
        val heartRate = (frame.heartRate ?: 0).toString()
        val power = (frame.power ?: 0).toString()
        val powerCal = frame.powerCal.toString()
        val lat = formatFixed(frame.lat ?: 0.0, 6)
        val lon = formatFixed(frame.lon ?: 0.0, 6)
        val altitude = formatFixedTrim(frame.altitudeM ?: 0.0, 2)
        val grade = formatFixedTrim(frame.gradeDeg, 2)
        val distanceKm = formatFixedTrim((frame.distanceM ?: 0.0) / 1000.0, 2)

        return listOf(
            param("PhoneTime", phoneTime, ""),
            param("FrameId", frame.frameId.toString(), ""),
            param("FPS", fps, "fps"),
            param("Speed", speed, "km/h"),
            param("SpeedGPS", speedGps, "km/h"),
            param("SpeedBLE", speedBle, "km/h"),
            param("SpeedSource", frame.speedSource, ""),
            param("SpeedSourceAgeMs", frame.speedSourceAgeMs.toString(), "ms"),
            param("SpeedConfidence", frame.speedConfidence.toString(), "pct"),
            param("Cadence", cadence, "rpm"),
            param("HeartRate", heartRate, "bpm"),
            param("HeartRateAgeMs", frame.hrAgeMs.toString(), "ms"),
            param("HeartRateConfidence", frame.hrConfidence.toString(), "pct"),
            param("Power", power, "watt"),
            param("PowerCal", powerCal, "watt"),
            param("GPS", lat, lon),
            param("GpsAgeMs", frame.gpsAgeMs.toString(), "ms"),
            param("GpsConfidence", frame.gpsConfidence.toString(), "pct"),
            param("Altitude", altitude, "m"),
            param("AltitudeAgeMs", frame.altitudeAgeMs.toString(), "ms"),
            param("AltitudeConfidence", frame.altitudeConfidence.toString(), "pct"),
            param("Grade", grade, "deg"),
            param("Distance", distanceKm, "km"),
            param("DataQuality", frame.dqRaw.toString(), "pct"),
            param("DataQualitySmooth", frame.dqSmooth.toString(), "pct"),
            param("DataQualityState", frame.dqState, ""),
            param("DataQualityReason", frame.dqReason, ""),
            param("DataQualityReasonShort", frame.dqReasonShort, "")
        )
    }

    private fun param(name: String, value: String, unit: String): VmixParam =
        VmixParam(ParameterName = name, Parameter_001 = value, Parameter_002 = unit)

    private fun formatFixed(value: Double, decimals: Int): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    private fun formatFixedTrim(value: Double, decimals: Int): String {
        val raw = formatFixed(value, decimals)
        val trimmed = raw.trimEnd('0').trimEnd('.').ifEmpty { "0" }
        return if (trimmed.contains('.')) trimmed else "${trimmed}.0"
    }

    private data class VmixParam(
        val ParameterName: String,
        val Parameter_001: String,
        val Parameter_002: String
    )
}
