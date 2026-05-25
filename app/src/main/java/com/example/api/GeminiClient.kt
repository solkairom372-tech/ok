package com.example.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateSpeechText(
        timeText: String,
        temperatureText: String,
        customMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            Log.w(TAG, "Gemini API key is default or missing. Returning simulated text.")
            return@withContext getLocalSimulatedText(timeText, temperatureText, customMessage)
        }

        val prompt = """
            Eres un asistente de voz de IA matutino e inspirador. Genera un saludo matutino animado y amigable exclusivamente EN ESPAÑOL para despertar al usuario.
            Incluye los siguientes datos del día de manera natural y conversacional:
            - Hora de despertar: $timeText
            - Temperatura estimada: $temperatureText
            - Una noticia curiosa o motivadora interesante del día que sea alegre (relleno entretenido).
            - Mensaje personalizado del usuario: "$customMessage" (si no está vacío, intégralo con cariño).
            Mantén el texto conciso, amigable, relajado y fluido. Ideal para ser leído en menos de 45 segundos. No uses formato Markdown, asteriscos, guiones ni caracteres extraños, solo texto liso fácil de pronunciar por un lector de pantalla (TTS).
        """.trimIndent()

        try {
            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObject = JSONObject()
            val partsArray = JSONArray()
            val partObject = JSONObject()
            
            partObject.put("text", prompt)
            partsArray.put(partObject)
            contentObject.put("parts", partsArray)
            contentsArray.put(contentObject)
            root.put("contents", contentsArray)

            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", "Eres una voz de IA cálida que despierta al usuario con noticias, clima y energía positiva."))
            systemInstruction.put("parts", sysParts)
            root.put("systemInstruction", systemInstruction)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response from Gemini: ${response.code} - ${response.message}")
                    return@withContext getLocalSimulatedText(timeText, temperatureText, customMessage)
                }

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Gemini response size: ${responseBody.length}")
                
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
                getLocalSimulatedText(timeText, temperatureText, customMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            getLocalSimulatedText(timeText, temperatureText, customMessage)
        }
    }

    private fun getLocalSimulatedText(time: String, temp: String, custom: String): String {
        val extraCustom = if (custom.isNotEmpty()) " Tu mensaje personal es: '$custom'." else ""
        val fillers = listOf(
            "Hoy es un día maravilloso para lograr tus metas. Recuerda tomar un vaso de agua al levantarte.",
            "La NASA ha visto una aurora espectacular hoy. Es un gran augurio para comenzar la mañana.",
            "El café ya está en camino, o al menos tu motivación para prepararlo de forma perfecta hoy.",
            "Una sonrisa libera endorfinas. Empieza estirando tus brazos con calma."
        )
        val randomFiller = fillers.random()
        return "¡Hola! Muy buenos días. Son las $time de la mañana. " +
                "La temperatura aproximada para iniciar el día es de $temp. " +
                "$randomFiller" +
                extraCustom +
                " ¡Paso a paso, hagamos de hoy un gran día! Ya puedes desactivar el despertador."
    }
}
