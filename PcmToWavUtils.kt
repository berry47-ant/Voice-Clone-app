package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PcmToWavUtils {

    /**
     * Converts raw PCM or encoded audio data to a valid playable audio file.
     * If the data is raw PCM (no RIFF / ID3 header), prepends a 44-byte WAV header.
     */
    fun saveAudioResponseToFile(
        context: Context,
        base64Data: String,
        mimeType: String?,
        outputFileName: String
    ): File {
        val audioDir = File(context.cacheDir, "audio").apply {
            if (!exists()) mkdirs()
        }
        val outputFile = File(audioDir, outputFileName)
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)

        // Check if bytes already have a WAV (RIFF) or MP3 (ID3/sync) header
        val isAlreadyWav = decodedBytes.size >= 4 &&
                decodedBytes[0] == 'R'.code.toByte() &&
                decodedBytes[1] == 'I'.code.toByte() &&
                decodedBytes[2] == 'F'.code.toByte() &&
                decodedBytes[3] == 'F'.code.toByte()

        val isMp3 = (decodedBytes.size >= 3 &&
                decodedBytes[0] == 'I'.code.toByte() &&
                decodedBytes[1] == 'D'.code.toByte() &&
                decodedBytes[2] == '3'.code.toByte()) ||
                (decodedBytes.size >= 2 &&
                        (decodedBytes[0].toInt() and 0xFF) == 0xFF &&
                        (decodedBytes[1].toInt() and 0xE0) == 0xE0)

        if (isAlreadyWav || isMp3) {
            outputFile.writeBytes(decodedBytes)
            return outputFile
        }

        // It is raw PCM (e.g., audio/pcm;rate=24000)
        // Parse sample rate if in mimeType (default to 24000Hz standard for Gemini TTS)
        var sampleRate = 24000
        mimeType?.let { mime ->
            val rateIndex = mime.indexOf("rate=")
            if (rateIndex != -1) {
                val rateStr = mime.substring(rateIndex + 5).takeWhile { it.isDigit() }
                rateStr.toIntOrNull()?.let { sampleRate = it }
            }
        }

        val channels = 1
        val bitsPerSample = 16
        val wavHeader = createWavHeader(decodedBytes.size, sampleRate, channels, bitsPerSample)

        FileOutputStream(outputFile).use { fos ->
            fos.write(wavHeader)
            fos.write(decodedBytes)
        }

        return outputFile
    }

    /**
     * Creates a standard 44-byte RIFF/WAVE header for 16-bit PCM audio.
     */
    private fun createWavHeader(
        pcmDataLength: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalDataLen = pcmDataLength + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF chunk descriptor
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())

        // "fmt " sub-chunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())

        // "data" sub-chunk
        header.put("data".toByteArray())
        header.putInt(pcmDataLength)

        return header.array()
    }

    /**
     * Exports audio to public device Downloads folder so the user can locate and keep it.
     * Returns true on success.
     */
    fun exportAudioToDownloads(
        context: Context,
        sourceFile: File,
        displayName: String
    ): Result<Uri> {
        return try {
            val contentResolver = context.contentResolver
            val isWav = sourceFile.name.endsWith(".wav", ignoreCase = true)
            val mimeType = if (isWav) "audio/wav" else "audio/mp4"

            val finalFileName = if (!displayName.endsWith(".wav") && !displayName.endsWith(".m4a") && !displayName.endsWith(".mp3")) {
                if (isWav) "$displayName.wav" else "$displayName.m4a"
            } else {
                displayName
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyanmarVoiceClone")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Failed to create download entry in MediaStore"))

                contentResolver.openOutputStream(uri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)

                Result.success(uri)
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "MyanmarVoiceClone").apply { if (!exists()) mkdirs() }
                val targetFile = File(targetDir, finalFileName)

                sourceFile.copyTo(targetFile, overwrite = true)
                val uri = Uri.fromFile(targetFile)
                Result.success(uri)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a share intent using FileProvider for sharing to messaging/social apps.
     */
    fun createShareIntent(context: Context, audioFile: File, title: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            audioFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = if (audioFile.name.endsWith(".wav")) "audio/wav" else "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Myanmar Cloned Voice Audio: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
