package com.example.alarm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object WakeUpSoundSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun playSound(soundName: String, volume: Float) {
        stop()
        playbackJob = scope.launch {
            try {
                val sampleRate = 22050
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.setVolume(volume)
                audioTrack?.play()

                Log.d("AudioSynth", "Synthesizer playing sound option: $soundName")
                when (soundName) {
                    "Amanecer Zen" -> playZenSunrise(sampleRate)
                    "Bosque Sereno" -> playSereneForest(sampleRate)
                    "Pulso Clásico" -> playClassicPulse(sampleRate)
                    else -> playSereneForest(sampleRate) // default
                }
            } catch (e: Exception) {
                Log.e("AudioSynth", "Error playing synthesized sound: ${e.message}", e)
            }
        }
    }

    fun setVolume(volume: Float) {
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            Log.e("AudioSynth", "Error setting volume: ${e.message}")
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null
    }

    private suspend fun CoroutineScope.playSereneForest(sampleRate: Int) {
        // Pentatonic peaceful sequence (Frequencies: E4, G4, A4, B4, D5) -> 329.63Hz, 392Hz, 440Hz, 493.88Hz, 587.33Hz
        val scale = doubleArrayOf(329.63, 392.00, 440.00, 493.88, 587.33)
        var noteIndex = 0
        
        while (isActive) {
            val freq = scale[noteIndex]
            val durationMs = 1500
            val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                // Exponential decay envelope
                val envelope = kotlin.math.exp(-3.5 * t)
                
                // Fundamental + 2nd harmonic (octave) + minor 3rd overtone
                val fundamental = sin(2 * Math.PI * freq * t)
                val octave = sin(2 * Math.PI * (freq * 2.0) * t) * 0.4
                val thirdOver = sin(2 * Math.PI * (freq * 1.5) * t) * 0.2
                
                val wave = (fundamental + octave + thirdOver) * envelope
                buffer[i] = (wave * 12000).toInt().toShort()
            }

            var offset = 0
            while (offset < buffer.size && isActive) {
                val written = audioTrack?.write(buffer, offset, buffer.size - offset) ?: 0
                if (written <= 0) break
                offset += written
            }
            
            delay(1200)
            noteIndex = (noteIndex + 1) % scale.size
        }
    }

    private suspend fun CoroutineScope.playZenSunrise(sampleRate: Int) {
        // 1. Slow swelling ocean tide waves (modulated ambient background)
        // 2. Beautiful high-frequency warm bell tones
        // Frequencies: G3, C4, D4, E4, G4 -> 196.0, 261.6, 293.6, 329.6, 392.0
        val scale = doubleArrayOf(196.0, 261.63, 293.66, 329.63, 392.00)
        var noteIndex = 0

        while (isActive) {
            val freq = scale[noteIndex]
            val durationMs = 2000
            val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                
                // Swelling ocean tide background sound (slow modulation of a low low frequency)
                val oceanWave = sin(2 * Math.PI * 65.0 * t) * sin(2 * Math.PI * 0.2 * t) * 0.15
                
                // Warm ambient glass plate note
                val decayEnv = kotlin.math.exp(-2.0 * t)
                val fundamental = sin(2 * Math.PI * freq * t)
                val ambientOvertone = sin(2 * Math.PI * (freq * 3.01) * t) * 0.12 // slightly out of tune for glass harmonic
                
                val wave = (fundamental + ambientOvertone) * decayEnv * 0.55 + oceanWave * 0.45
                buffer[i] = (wave * 13000).toInt().toShort()
            }

            var offset = 0
            while (offset < buffer.size && isActive) {
                val written = audioTrack?.write(buffer, offset, buffer.size - offset) ?: 0
                if (written <= 0) break
                offset += written
            }
            
            delay(1600)
            noteIndex = (noteIndex + 1) % scale.size
        }
    }

    private suspend fun CoroutineScope.playClassicPulse(sampleRate: Int) {
        // High frequency beeping alarms
        while (isActive) {
            val freq = 2000.0 // crisp loud alarming beep
            val durationMs = 150
            val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                buffer[i] = (sin(2 * Math.PI * freq * t) * 14000).toInt().toShort()
            }

            var offset = 0
            while (offset < buffer.size && isActive) {
                val written = audioTrack?.write(buffer, offset, buffer.size - offset) ?: 0
                if (written <= 0) break
                offset += written
            }
            
            delay(200)

            offset = 0
            while (offset < buffer.size && isActive) {
                val written = audioTrack?.write(buffer, offset, buffer.size - offset) ?: 0
                if (written <= 0) break
                offset += written
            }

            delay(1200) // Pause between alerts
        }
    }
}
