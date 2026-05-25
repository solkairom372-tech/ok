package com.example.sleep

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class SleepTrackerService : Service() {
    private var receiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("SleepTrackerService", "Service started. Registering Screen Off/On receivers.")
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val prefs = context.getSharedPreferences("sleep_tracker_prefs", Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d("SleepTracker", "Screen OFF detected. Marking sleep start.")
                        prefs.edit().putLong("last_screen_off_time", now).apply()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        Log.d("SleepTracker", "Screen ON detected. Marking potential wake/ring started.")
                        prefs.edit().putLong("last_screen_on_time", now).apply()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        Log.d("SleepTracker", "User PRESENT (unlocked) detected.")
                        prefs.edit().putLong("last_user_present_time", now).apply()
                    }
                }
            }
        }
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        receiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e("SleepTrackerService", "Error unregistering receiver", e)
            }
        }
        Log.d("SleepTrackerService", "Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
