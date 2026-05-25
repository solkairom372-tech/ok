package com.example.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm triggers! Opening Active Alarm screen.")
        
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_ACTION", "ALARM_RING")
            putExtra("EXTRA_ALARM_ID", intent.getIntExtra("EXTRA_ALARM_ID", -1))
            putExtra("EXTRA_CUSTOM_MSG", intent.getStringExtra("EXTRA_CUSTOM_MSG") ?: "")
            putExtra("EXTRA_SOUND_NAME", intent.getStringExtra("EXTRA_SOUND_NAME") ?: "Bosque Sereno")
        }
        
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Cannot start MainActivity directly, falling back to notification.", e)
        }

        val prefs = context.getSharedPreferences("sleep_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_alarm_trigger_time", System.currentTimeMillis()).apply()

        NotificationHelper.createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            111,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("¡Despierta! ⏰")
            .setContentText("Tu despertador inteligente está sonando...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(555, builder.build())
    }
}
