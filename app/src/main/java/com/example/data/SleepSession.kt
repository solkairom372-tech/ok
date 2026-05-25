package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val wakeTime: Long,
    val sleepDurationMillis: Long,
    val dismissLatencyMillis: Long,
    val qualityScore: Int,
    val dateLabel: String
)
