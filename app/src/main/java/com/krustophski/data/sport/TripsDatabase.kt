package com.krustophski.data.sport

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.krustophski.data.sport.data.CadenceSpeedMeasurement
import com.krustophski.data.sport.data.CadenceSpeedMeasurementDao
import com.krustophski.data.sport.data.Export
import com.krustophski.data.sport.data.ExportDao
import com.krustophski.data.sport.data.HeartRateMeasurement
import com.krustophski.data.sport.data.HeartRateMeasurementDao
import com.krustophski.data.sport.data.SensorTypeConverter

@Database(
    entities = [
        Trip::class,
        Measurements::class,
        TimeState::class,
        Split::class,
        OnboardSensors::class,
        Bike::class,
        ExternalSensor::class,
        Weather::class,
        HeartRateMeasurement::class,
        CadenceSpeedMeasurement::class,
        Export::class
    ],
    version = 29
)
@TypeConverters(
    TimeStateEnumConverter::class,
    UserSexEnumConverter::class,
    GoogleFitSyncStatusConverter::class,
    SensorTypeConverter::class
)
abstract class TripsDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun measurementsDao(): MeasurementsDao
    abstract fun timeStateDao(): TimeStateDao
    abstract fun splitDao(): SplitDao
    abstract fun onboardSensorsDao(): OnboardSensorsDao
    abstract fun bikeDao(): BikeDao
    abstract fun externalSensorsDao(): ExternalSensorDao
    abstract fun weatherDao(): WeatherDao
    abstract fun heartRateMeasurementDao(): HeartRateMeasurementDao
    abstract fun cadenceSpeedMeasurementDao(): CadenceSpeedMeasurementDao
    abstract fun exportDao(): ExportDao
}
