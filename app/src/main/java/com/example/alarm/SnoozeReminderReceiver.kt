package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SnoozeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SnoozeReminderReceiver", "Alarm wake check triggered after 5-10 minutes.")
        NotificationHelper.showReminderNotification(context)
    }
}
