package com.example.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao, private val sleepSessionDao: SleepSessionDao) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    val allSessions: Flow<List<SleepSession>> = sleepSessionDao.getAllSessions()

    suspend fun getEnabledAlarms(): List<Alarm> = alarmDao.getEnabledAlarms()

    suspend fun insertAlarm(alarm: Alarm): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = alarmDao.deleteAlarm(alarm)

    suspend fun insertSession(session: SleepSession) = sleepSessionDao.insertSession(session)

    suspend fun clearSessions() = sleepSessionDao.clearAll()
}
