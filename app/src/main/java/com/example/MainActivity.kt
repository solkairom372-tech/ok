package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmViewModel
import com.example.alarm.NotificationHelper
import com.example.sleep.SleepTrackerService
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: AlarmViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // 1. Initialize Notification Channels
    NotificationHelper.createNotificationChannels(this)

    // 2. Request Notification Permission for API 33+ (Mandatory)
    requestNotificationPermission()

    // 3. Start background sleep logger service dynamically
    try {
      val serviceIntent = Intent(this, SleepTrackerService::class.java)
      startService(serviceIntent)
    } catch (e: Exception) {
      Log.e("MainActivity", "Error starting SleepTrackerService", e)
    }

    // 4. Intercept potential ringing intents on boot
    handleAlarmIntent(intent)

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainDashboard(
            viewModel = viewModel
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleAlarmIntent(intent)
  }

  private fun handleAlarmIntent(intent: Intent?) {
    if (intent == null) return
    val action = intent.getStringExtra("EXTRA_ACTION")
    if (action == "ALARM_RING") {
      val alarmId = intent.getIntExtra("EXTRA_ALARM_ID", -1)
      val customMsg = intent.getStringExtra("EXTRA_CUSTOM_MSG") ?: ""
      val soundName = intent.getStringExtra("EXTRA_SOUND_NAME") ?: "Bosque Sereno"
      Log.d("MainActivity", "Intercepted Ringing Alarm Intent. Triggering Alarm screen.")
      viewModel.startRinging(alarmId, customMsg, soundName)
    } else if (action == "CONFIRM_AWAKE") {
      Log.d("MainActivity", "User confirmed they are awake from safety check.")
      val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
      notificationManager.cancel(777)
    }
  }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = Manifest.permission.POST_NOTIFICATIONS
      if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), 222)
      }
    }
  }
}
