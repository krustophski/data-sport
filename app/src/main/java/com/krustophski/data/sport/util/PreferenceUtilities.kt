package com.krustophski.data.sport.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.krustophski.data.sport.FeatureFlags
import com.krustophski.data.sport.R
import com.krustophski.data.sport.userCircumferenceToMeters
import kotlin.math.roundToInt

fun putSafeZoneMargins(context: Context, margins: Rect) =
    PreferenceManager.getDefaultSharedPreferences(context).edit {
        putInt(context.getString(R.string.preferences_dashboard_safe_zone_top_margin), margins.top)
        putInt(
            context.getString(R.string.preferences_dashboard_safe_zone_bottom_margin),
            margins.bottom
        )
        putInt(
            context.getString(R.string.preferences_dashboard_safe_zone_left_margin),
            margins.left
        )
        putInt(
            context.getString(R.string.preferences_dashboard_safe_zone_right_margin),
            margins.right
        )
        commit()
    }

fun getSafeZoneMargins(context: Context): Rect {
    val prefs = getPreferences(context)
    val keyTop = context.getString(R.string.preferences_dashboard_safe_zone_top_margin)
    val keyBottom = context.getString(R.string.preferences_dashboard_safe_zone_bottom_margin)
    val keyLeft = context.getString(R.string.preferences_dashboard_safe_zone_left_margin)
    val keyRight = context.getString(R.string.preferences_dashboard_safe_zone_right_margin)

    val hasAny = prefs.contains(keyTop) || prefs.contains(keyBottom) ||
        prefs.contains(keyLeft) || prefs.contains(keyRight)

    if (!hasAny) {
        val metrics = context.resources.displayMetrics
        val defaultMarginH = (metrics.widthPixels * 0.15f).roundToInt()
        val defaultMarginV = (metrics.heightPixels * 0.15f).roundToInt()
        val defaults = Rect(defaultMarginH, defaultMarginV, defaultMarginH, defaultMarginV)
        putSafeZoneMargins(context, defaults)
        return defaults
    }

    return Rect(
        getSafeZoneLeftMarginPreference(context),
        getSafeZoneTopMarginPreference(context),
        getSafeZoneRightMarginPreference(context),
        getSafeZoneBottomMarginPreference(context),
    )
}

fun setSafeZoneTopMarginPreference(context: Context, margin: Int) =
    PreferenceManager.getDefaultSharedPreferences(context).edit {
        putInt(context.getString(R.string.preferences_dashboard_safe_zone_top_margin), margin)
        commit()
    }

fun setSafeZoneBottomMarginPreference(context: Context, margin: Int) =
    PreferenceManager.getDefaultSharedPreferences(context).edit {
        putInt(context.getString(R.string.preferences_dashboard_safe_zone_bottom_margin), margin)
        commit()
    }

fun getSafeZoneTopMarginPreference(context: Context): Int =
    getPreferences(context).getInt(
        context.getString(R.string.preferences_dashboard_safe_zone_top_margin), 0
    )

fun getSafeZoneBottomMarginPreference(context: Context): Int =
    getPreferences(context).getInt(
        context.getString(R.string.preferences_dashboard_safe_zone_bottom_margin), 0
    )

fun getSafeZoneLeftMarginPreference(context: Context): Int =
    getPreferences(context).getInt(
        context.getString(R.string.preferences_dashboard_safe_zone_left_margin), 0
    )

fun getSafeZoneRightMarginPreference(context: Context): Int =
    getPreferences(context).getInt(
        context.getString(R.string.preferences_dashboard_safe_zone_right_margin), 0
    )

fun getBrightnessPreference(context: Context): Float {
    return if (getPreferences(context)
            .getBoolean(
                context.getString(R.string.preferences_dashboard_brightness_toggle_key),
                false
            )
    ) {
        getPreferences(context)
            .getInt(context.getString(R.string.preferences_dashboard_brightness_key), 0) / 100f
    } else -1f
}

fun userMassToKilograms(context: Context, input: String?): Float? {
    return try {
        (input?.toFloat()
            ?: Float.NEGATIVE_INFINITY) * (when (PreferenceManager.getDefaultSharedPreferences(
            context
        )
            .getString("display_units", "1")) {
            "1" -> POUNDS_TO_KG
            else -> 1.0
        }).toFloat()
    } catch (e: NumberFormatException) {
        Log.e("TRIP_UTILS_PREF", "userCircumferenceToMeters: Couldn't parse wheel circumference")
        null
    }
}

fun getBikeMassOrNull(context: Context): Float? {
    return userMassToKilograms(
        context,
        getBikeMassOrNull(
            getPreferences(context),
            context.getString(R.string.preference_key_bike_mass)
        )
    )
}

fun getUserCircumferenceOrNull(context: Context): Float? =
    userCircumferenceToMeters(
        getPreferences(context).getString(
            context.getString(R.string.preference_key_wheel_circumference),
            ""
        )
    )

fun getBikeMassOrNull(prefs: SharedPreferences, key: String): String? {
    val stored = prefs.getString(key, "")
    Log.d("PreferenceUtilities", "Bike mass preference: $stored")
    return stored
}

fun metersToUserCircumference(context: Context, meters: Float): String {
    return metersToUserCircumference(meters, getPreferences(context))
}

fun metersToUserCircumference(meters: Float, prefs: SharedPreferences): String {
    val storedCircumference = prefs.getString("wheel_circumference", "2037")
    return com.krustophski.data.sport.metersToUserCircumference(meters, storedCircumference)
}

fun getSystemOfMeasurement(context: Context): String? =
    getPreferences(context)
        .getString("display_units", "1")

fun getPreferences(context: Context): SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)

fun useGoogleFitRestingHeartRate(context: Context) = getPreferences(context).getBoolean(
    context.getString(R.string.preferences_key_advanced_enable_google_fit_resting_heart_rate),
    false
)

fun useGoogleFitBiometrics(context: Context) = getPreferences(context).getBoolean(
    context.getString(R.string.preference_key_biometrics_use_google_fit_biometrics),
    true
)

fun shouldSyncGoogleFitBiometrics(context: Context) = getPreferences(context).getBoolean(
    context.getString(R.string.preferences_key_advanced_enable_google_fit_sync_biometrics),
    FeatureFlags.betaBuild
)

fun shouldCollectOnboardSensors(context: Context) = getPreferences(context).getBoolean(
    context.getString(R.string.preferences_key_advanced_onboard_sensors),
    FeatureFlags.betaBuild
)

fun useVo2maxCalorieEstimate(context: Context) = getPreferences(context).getBoolean(
    context.getString(R.string.preferences_key_advanced_use_vo2max_calorie_estimate), false
)

fun isStravaSynced(context: Context) = getPreferences(context).getString(
    context.getString(R.string.preference_key_strava_refresh_token),
    null
).isNullOrBlank().not()
