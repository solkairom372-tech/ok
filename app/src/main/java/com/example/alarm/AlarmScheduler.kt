package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Alarm
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms! Permission is missing. Scheduling with setAndAllowWhileIdle/exact fallback.")
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarm.id)
            putExtra("EXTRA_CUSTOM_MSG", alarm.customMessage)
            putExtra("EXTRA_SOUND_NAME", alarm.soundName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        Log.d(TAG, "Scheduling alarm for: ${calendar.time} (Alarm ID: ${alarm.id})")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm. Falling back to non-exact.", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (pendingIntent != null) {
            Log.d(TAG, "Canceling scheduled alarm ID: ${alarm.id}")
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleAwakeCheck(context: Context, checkMinutes: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SnoozeReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val triggerTime = System.currentTimeMillis() + (checkMinutes * 60 * 1000)
        Log.d(TAG, "Scheduling awake check in $checkMinutes minutes...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
