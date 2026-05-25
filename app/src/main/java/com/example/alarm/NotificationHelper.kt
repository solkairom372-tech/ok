package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {
    const val ALARM_CHANNEL_ID = "INTELLIWAKE_ALARMS_CHANNEL"
    const val REMINDER_CHANNEL_ID = "INTELLIWAKE_REMINDER_CHANNEL"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Despertador IA Alertas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para alarmas activas."
                setSound(null, null)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(alarmChannel)

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Despertador IA Confirmación",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para asegurar que sigas despierto."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showReminderNotification(context: Context) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_ACTION", "CONFIRM_AWAKE")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("¿Sigues despierto? ☕")
            .setContentText("Presiona aquí para confirmar que no te has vuelto a dormir.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(777, builder.build())
    }
}
