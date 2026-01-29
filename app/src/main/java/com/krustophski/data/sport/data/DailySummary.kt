package com.krustophski.data.sport.data

import java.time.ZonedDateTime

data class DailySummary(
    val date: ZonedDateTime,
    val distance: Double? = null,
    val duration: Double? = null,
)
