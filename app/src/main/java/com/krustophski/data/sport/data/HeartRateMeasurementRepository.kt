package com.krustophski.data.sport.data

import javax.inject.Inject

class HeartRateMeasurementRepository @Inject constructor(private val heartRateMeasurementDao: HeartRateMeasurementDao) {
    suspend fun save(new: HeartRateMeasurement) = heartRateMeasurementDao.save(new)
    suspend fun get(tripId: Long) = heartRateMeasurementDao.load(tripId)
    fun observeTripHeartRates(tripId: Long) = heartRateMeasurementDao.subscribe(tripId)

    suspend fun changeTrip(tripId: Long, newTripId: Long) =
        heartRateMeasurementDao.changeTrip(tripId, newTripId)
}
