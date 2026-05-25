package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_sessions ORDER BY endTime DESC")
    fun getAllSessions(): Flow<List<SleepSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSession)

    @Query("DELETE FROM sleep_sessions")
    suspend fun clearAll()
}
