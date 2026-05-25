package com.example.alarm

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.Alarm
import com.example.data.AppDatabase
import com.example.data.AlarmRepository
import com.example.data.SleepSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    val alarms: StateFlow<List<Alarm>>
    val sessions: StateFlow<List<SleepSession>>

    // UI States
    val isAlarmActive = MutableStateFlow(false)
    val activeAlarmId = MutableStateFlow(-1)
    val isSpeechLoading = MutableStateFlow(false)
    val spokenText = MutableStateFlow("")
    val isSleepModeActive = MutableStateFlow(false)
    
    // Last sleep result modal trigger
    val lastSessionSaved = MutableStateFlow<SleepSession?>(null)

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var vibrator: Vibrator? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AlarmRepository(database.alarmDao(), database.sleepSessionDao())
        
        alarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        sessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        // Initialize TTS in Spanish
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeResult = tts?.setLanguage(Locale("es", "ES"))
                if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("AlarmViewModel", "Spanish language is not supported or missing offline data. Falling back to default locale.")
                    tts?.setLanguage(Locale.getDefault())
                }
                
                // Try to find a premium neural/AI voice for Spanish
                try {
                    val voices = tts?.voices
                    val aiVoice = voices?.find { 
                        it.locale.language == "es" && (it.isNetworkConnectionRequired || it.features.contains("highQuality")) 
                    } ?: voices?.find { it.locale.language == "es" }
                    if (aiVoice != null) {
                        tts?.voice = aiVoice
                        Log.d("AlarmViewModel", "Premium AI voice selected: ${aiVoice.name}")
                    }
                } catch (e: Exception) {
                    Log.w("AlarmViewModel", "Could not set custom voice: ${e.message}")
                }

                isTtsInitialized = true
                
                // Adjust speech rate slightly slower for a relaxing wake up voice
                tts?.setSpeechRate(0.85f)
            } else {
                Log.e("AlarmViewModel", "Failed to initialize TTS engine.")
            }
        }
    }

    // --- ALARM MANAGEMENT ---

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(isEnabled = enabled)
            repository.updateAlarm(updated)
            
            withContext(Dispatchers.Main) {
                if (enabled) {
                    AlarmScheduler.scheduleAlarm(getApplication(), updated)
                } else {
                    AlarmScheduler.cancelAlarm(getApplication(), updated)
                }
            }
        }
    }

    fun addAlarm(hour: Int, minute: Int, customMsg: String, soundName: String = "Bosque Sereno") {
        viewModelScope.launch(Dispatchers.IO) {
            val freshAlarm = Alarm(hour = hour, minute = minute, customMessage = customMsg, isEnabled = true, soundName = soundName)
            val generatedId = repository.insertAlarm(freshAlarm)
            val alarmWithId = freshAlarm.copy(id = generatedId.toInt())
            
            withContext(Dispatchers.Main) {
                AlarmScheduler.scheduleAlarm(getApplication(), alarmWithId)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlarm(alarm)
            withContext(Dispatchers.Main) {
                AlarmScheduler.cancelAlarm(getApplication(), alarm)
            }
        }
    }

    // --- SLEEP MODE MANUAL INITIATION ---
    fun startSleepMode() {
        val prefs = getApplication<Application>().getSharedPreferences("sleep_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_screen_off_time", System.currentTimeMillis()).apply()
        isSleepModeActive.value = true
    }

    fun stopSleepMode() {
        isSleepModeActive.value = false
    }

    // --- ACTIVE RINGING OPERATIONS ---

    fun startRinging(alarmId: Int, customMsg: String, soundName: String = "Bosque Sereno") {
        if (isAlarmActive.value) return // Already ringing
        Log.d("AlarmViewModel", "Ringing alarm ID: $alarmId. Msg: $customMsg. Sound: $soundName")
        
        isAlarmActive.value = true
        activeAlarmId.value = alarmId
        spokenText.value = ""
        isSpeechLoading.value = true

        // Register ProgressListener to handle automatic ducking/volume restoration
        setupTtsProgressListener()

        // 1. Play Alarm tone or Soundscape
        if (soundName == "Tono del Sistema") {
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(getApplication(), alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    setVolume(0.12f, 0.12f) // Start ducked, low-volume background
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e("AlarmViewModel", "Error playing system ringtone", e)
            }
        } else {
            WakeUpSoundSynthesizer.playSound(soundName, 0.12f)
        }

        // 2. Continuous vibrating pattern
        startVibrating()

        // 3. Gather Day Stats (Time, simulated temp)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeNow = sdf.format(Date())
        val tempSim = simulateMorningTemperature()

        // 4. Fetch speech message from Gemini API
        viewModelScope.launch {
            val generatedSpeech = GeminiClient.generateSpeechText(
                timeText = timeNow,
                temperatureText = tempSim,
                customMessage = customMsg
            )
            isSpeechLoading.value = false
            spokenText.value = generatedSpeech

            // Speak generated greeting out loud in Spanish
            if (isTtsInitialized) {
                // Keep background music extra quiet while talking so TTS is MUCH stronger
                mediaPlayer?.setVolume(0.01f, 0.01f)
                WakeUpSoundSynthesizer.setVolume(0.01f)
                
                val params = android.os.Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                tts?.speak(generatedSpeech, TextToSpeech.QUEUE_FLUSH, params, "WAKE_UP_SPEECH")
                
                // Track TTS completion (or fallback wait before restoring sound levels)
                delay(35000)
                mediaPlayer?.setVolume(0.15f, 0.15f)
                WakeUpSoundSynthesizer.setVolume(0.15f)
            }
        }
    }

    private fun setupTtsProgressListener() {
        if (isTtsInitialized) {
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mediaPlayer?.setVolume(0.01f, 0.01f)
                    WakeUpSoundSynthesizer.setVolume(0.01f)
                }

                override fun onDone(utteranceId: String?) {
                    mediaPlayer?.setVolume(0.15f, 0.15f)
                    WakeUpSoundSynthesizer.setVolume(0.15f)
                }

                override fun onError(utteranceId: String?) {
                    mediaPlayer?.setVolume(0.15f, 0.15f)
                    WakeUpSoundSynthesizer.setVolume(0.15f)
                }
            })
        }
    }

    private fun startVibrating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 800, 800)
            val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 800, 800), 0)
        }
    }

    private fun simulateMorningTemperature(): String {
        // Simple ambient temperature simulator
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val baseTemp = if (hour in 5..9) 14 else if (hour in 10..18) 22 else 17
        val variation = (-2..2).random()
        return "${baseTemp + variation}°C"
    }

    fun dismissAlarm() {
        if (!isAlarmActive.value) return
        Log.d("AlarmViewModel", "Alarm dismissed. Saving stats...")

        // 1. Silent everything
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        WakeUpSoundSynthesizer.stop()
        tts?.stop()
        vibrator?.cancel()

        isAlarmActive.value = false
        isSleepModeActive.value = false

        // 2. Clear heads-up notification
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(555)

        // 3. EVALUATE SLEEP QUALITY AND RECORD SESSIONS
        val now = System.currentTimeMillis()
        val prefs = getApplication<Application>().getSharedPreferences("sleep_tracker_prefs", Context.MODE_PRIVATE)
        
        // Bed start: Check manual screen/sleep button, or dynamic broadcast screen-off
        var startTime = prefs.getLong("last_screen_off_time", 0)
        
        // Wake start (last_alarm_trigger_time or screen ON)
        val wakeTime = prefs.getLong("last_alarm_trigger_time", now)

        // Latency
        val latency = now - wakeTime

        // Fallback sleep duration
        if (startTime == 0L || startTime > wakeTime) {
            // Assume 8 hours default sleep if they did not lock phone/enable sleep mode
            startTime = wakeTime - (8 * 60 * 60 * 1000)
        }

        val sleepDuration = now - startTime

        // Compute Sleep Quality Score
        val quality = computeSleepQuality(sleepDuration, latency)

        val dateLabelStr = SimpleDateFormat("EEE d MMM", Locale("es", "ES")).format(Date())

        val session = SleepSession(
            startTime = startTime,
            endTime = now,
            wakeTime = wakeTime,
            sleepDurationMillis = sleepDuration,
            dismissLatencyMillis = latency,
            qualityScore = quality,
            dateLabel = dateLabelStr
        )

        // Save session
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSession(session)
            // Expose to display beautiful morning summary cards
            lastSessionSaved.value = session
        }

        // 4. SCHEDULE THE AWAKE CHECK IN 5 MINUTES
        // (As requested: "despues de 5 a 10 minutos de despertar te mande notificaciones preguntando si sigues despierto")
        AlarmScheduler.scheduleAwakeCheck(getApplication(), checkMinutes = 5)
    }

    private fun computeSleepQuality(durationMillis: Long, latencyMillis: Long): Int {
        val durationHours = durationMillis.toDouble() / (1000 * 60 * 60)
        
        // Target: 8 Hours.
        var durationScore = 80
        if (durationHours in 7.0..9.0) {
            durationScore = 80
        } else if (durationHours < 7.0) {
            val shortDebt = (7.0 - durationHours) * 10
            durationScore = (80 - shortDebt).toInt().coerceAtLeast(30)
        } else {
            val longSurplus = (durationHours - 9.0) * 5
            durationScore = (80 - longSurplus).toInt().coerceAtLeast(50)
        }

        // Speed of waking up (latency). Quickly turns off: +20 points. Slow: penalty.
        val latencyMinutes = latencyMillis.toDouble() / (1000 * 60)
        var latencyScore = 20
        if (latencyMinutes > 1.0) {
            val penalty = (latencyMinutes - 1.0) * 4
            latencyScore = (20 - penalty).toInt().coerceAtLeast(0)
        }

        return (durationScore + latencyScore).coerceIn(0, 100)
    }

    fun clearSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSessions()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
        WakeUpSoundSynthesizer.stop()
        tts?.shutdown()
        vibrator?.cancel()
    }
}
