package com.example.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.audio.PcmToWavUtils
import com.example.model.AudioClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiTtsService(private val context: Context) {

    companion object {
        private const val TAG = "GeminiTtsService"
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        const val FALLBACK_MODEL = "gemini-1.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateClonedSpeech(
        apiKey: String,
        myanmarText: String,
        voiceSampleFile: File?,
        model: String = DEFAULT_MODEL,
        voiceStyle: String = "Natural",
        prebuiltVoice: String = "Aoede"
    ): Result<AudioClip> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API Key is missing. Please provide a valid Gemini API Key in Settings or .env.")
            )
        }

        if (myanmarText.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Please enter Myanmar text to synthesize.")
            )
        }

        try {
            // First attempt with specified model
            val result = executeGenerateRequest(apiKey, myanmarText, voiceSampleFile, model, voiceStyle, prebuiltVoice)
            if (result.isSuccess) {
                return@withContext result
            }

            // If the model was gemini-3.1-flash-tts and resulted in a 404/not found, try fallback model
            val errorMsg = result.exceptionOrNull()?.message ?: ""
            if (model == DEFAULT_MODEL && (errorMsg.contains("404") || errorMsg.contains("not found", ignoreCase = true))) {
                Log.w(TAG, "Model $model returned 404, falling back to $FALLBACK_MODEL")
                return@withContext executeGenerateRequest(apiKey, myanmarText, voiceSampleFile, FALLBACK_MODEL, voiceStyle, prebuiltVoice)
            }

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Speech generation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun executeGenerateRequest(
        apiKey: String,
        myanmarText: String,
        voiceSampleFile: File?,
        modelName: String,
        voiceStyle: String,
        prebuiltVoice: String
    ): Result<AudioClip> {
        val endpointUrl = "$BASE_URL$modelName:generateContent?key=$apiKey"

        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        contentObj.put("role", "user")

        val partsArray = JSONArray()

        val hasVoiceSample = voiceSampleFile != null && voiceSampleFile.exists() && voiceSampleFile.length() > 0

        // 1. Text Prompt part with Myanmar cloning instructions
        val promptText = if (hasVoiceSample) {
            """
            Role: Expert Myanmar (Burmese) Voice Cloner and Speech Synthesizer.
            Task: Clone the vocal timbre, pitch, gender, speaking pace, resonance, and Myanmar native pronunciation of the speaker in the attached voice sample audio.
            Tone: $voiceStyle.
            Please synthesize and articulate the following Myanmar (Burmese) Unicode text using the exact cloned voice characteristics:
            
            Text:
            $myanmarText
            """.trimIndent()
        } else {
            """
            Role: Expert Myanmar (Burmese) Speech Synthesizer.
            Task: Read the following Myanmar (Burmese) text naturally, fluently, and clearly with authentic native Myanmar pronunciation.
            Tone: $voiceStyle.
            
            Text:
            $myanmarText
            """.trimIndent()
        }

        val textPart = JSONObject()
        textPart.put("text", promptText)
        partsArray.put(textPart)

        // 2. Audio part if voice sample exists
        if (hasVoiceSample) {
            val audioBytes = voiceSampleFile!!.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", "audio/mp4")
            inlineDataObj.put("data", base64Audio)

            val audioPart = JSONObject()
            audioPart.put("inlineData", inlineDataObj)
            partsArray.put(audioPart)
        }

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        // 3. Generation Config requesting AUDIO modality
        val generationConfig = JSONObject()
        val modalitiesArray = JSONArray()
        modalitiesArray.put("AUDIO")
        generationConfig.put("responseModalities", modalitiesArray)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", prebuiltVoice)
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        generationConfig.put("speechConfig", speechConfig)

        requestJson.put("generationConfig", generationConfig)

        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(endpointUrl)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        val responseBodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMessage = try {
                val errObj = JSONObject(responseBodyString).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
            } catch (e: Exception) {
                "HTTP ${response.code}: $responseBodyString"
            }
            return Result.failure(Exception(errorMessage))
        }

        return parseAudioFromResponse(responseBodyString, myanmarText, modelName, hasVoiceSample)
    }

    private fun parseAudioFromResponse(
        jsonString: String,
        myanmarText: String,
        modelName: String,
        isCloned: Boolean
    ): Result<AudioClip> {
        try {
            val rootObj = JSONObject(jsonString)
            val candidates = rootObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return Result.failure(Exception("No speech candidate returned by Gemini."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts == null || parts.length() == 0) {
                return Result.failure(Exception("Empty speech content returned by Gemini."))
            }

            var base64AudioData: String? = null
            var mimeType: String? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null && inlineData.has("data")) {
                    base64AudioData = inlineData.getString("data")
                    mimeType = inlineData.optString("mimeType", "audio/wav")
                    break
                }
            }

            if (base64AudioData.isNullOrBlank()) {
                return Result.failure(Exception("Response did not contain audio stream data."))
            }

            val clipId = UUID.randomUUID().toString().take(8)
            val outputFileName = "cloned_speech_${System.currentTimeMillis()}_$clipId.wav"
            val savedAudioFile = PcmToWavUtils.saveAudioResponseToFile(
                context = context,
                base64Data = base64AudioData,
                mimeType = mimeType,
                outputFileName = outputFileName
            )

            val displayTitle = if (myanmarText.length > 28) {
                myanmarText.take(28) + "..."
            } else {
                myanmarText
            }

            // Estimate duration or read from file
            val durationMs = estimateWavDurationMs(savedAudioFile)

            val audioClip = AudioClip(
                id = clipId,
                title = displayTitle,
                textPrompt = myanmarText,
                filePath = savedAudioFile.absolutePath,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis(),
                isCloned = isCloned,
                modelUsed = modelName
            )

            return Result.success(audioClip)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing audio response: ${e.message}", e)
            return Result.failure(e)
        }
    }

    private fun estimateWavDurationMs(file: File): Long {
        return try {
            val bytes = file.length()
            if (bytes > 44) {
                // 24000 Hz * 1 channel * 2 bytes/sample = 48000 bytes/sec
                val pcmBytes = bytes - 44
                ((pcmBytes.toDouble() / 48000.0) * 1000).toLong()
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
