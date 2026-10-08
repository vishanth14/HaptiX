package com.haptix.app.domain.service

import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.domain.model.ProcessingJob
import com.haptix.app.domain.model.ProcessingStatus
import com.haptix.app.domain.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service contract for submitting YouTube video URLs to the offline/backend multimodal synthesis pipeline.
 */
interface HapticProcessingService {
    suspend fun submitJob(url: String): Result<ProcessingJob>
    suspend fun getJobStatus(jobId: String): Result<ProcessingJob>
    suspend fun getGeneratedPattern(jobId: String): Result<HapticPattern>
}

/**
 * Client implementation communicating with the HaptiX local processing backend over HTTP.
 *
 * Configurable base URL:
 * - Default: `http://10.0.2.2:8080` (standard Android Emulator loopback to host)
 * - Custom: `http://<HOST_IP>:8080` for physical devices over local Wi-Fi.
 */
class RemoteHapticProcessingService(
    private val baseUrl: String = "http://10.0.2.2:8080"
) : HapticProcessingService {

    override suspend fun submitJob(url: String): Result<ProcessingJob> = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$baseUrl/api/jobs/youtube")
            val conn = endpoint.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("url", url)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val jobId = json.getString("jobId")
                val statusStr = json.optString("status", "QUEUED")
                val progress = json.optDouble("progress", 0.0).toFloat()
                val stage = json.optString("stage", "Job accepted")

                Result.success(
                    ProcessingJob(
                        jobId = jobId,
                        videoSource = VideoSource.YouTube(url),
                        status = parseStatus(statusStr),
                        progress = progress,
                        stageMessage = stage
                    )
                )
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Result.failure(RuntimeException("Job submission rejected: $errorText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getJobStatus(jobId: String): Result<ProcessingJob> = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$baseUrl/api/jobs/$jobId")
            val conn = endpoint.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5_000
            conn.readTimeout = 10_000

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val url = json.optString("url", "https://www.youtube.com/watch?v=${jobId.removePrefix("yt_")}")
                val statusStr = json.optString("status", "QUEUED")
                val progress = json.optDouble("progress", 0.0).toFloat()
                val stage = json.optString("stage", "Processing")
                val error = if (json.has("errorMessage") && !json.isNull("errorMessage")) json.getString("errorMessage") else null

                Result.success(
                    ProcessingJob(
                        jobId = jobId,
                        videoSource = VideoSource.YouTube(url),
                        status = parseStatus(statusStr),
                        progress = progress,
                        stageMessage = stage,
                        errorMessage = error
                    )
                )
            } else {
                Result.failure(RuntimeException("Status check returned HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGeneratedPattern(jobId: String): Result<HapticPattern> = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$baseUrl/api/jobs/$jobId/pattern")
            val conn = endpoint.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 8_000
            conn.readTimeout = 15_000

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                FrequencyPatternParser.parseToHapticPattern(responseText)
            } else {
                Result.failure(RuntimeException("Pattern retrieval returned HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStatus(str: String): ProcessingStatus {
        return try {
            ProcessingStatus.valueOf(str.uppercase())
        } catch (_: Exception) {
            when {
                str.contains("DOWN", ignoreCase = true) -> ProcessingStatus.DOWNLOADING
                str.contains("VIDEO", ignoreCase = true) -> ProcessingStatus.VIDEO_ANALYSIS
                str.contains("AUDIO", ignoreCase = true) -> ProcessingStatus.AUDIO_ANALYSIS
                str.contains("FUSION", ignoreCase = true) -> ProcessingStatus.FUSION
                str.contains("HAPTIC", ignoreCase = true) -> ProcessingStatus.HAPTIC_GENERATION
                str.contains("COMPLET", ignoreCase = true) || str.contains("READY", ignoreCase = true) -> ProcessingStatus.COMPLETED
                str.contains("FAIL", ignoreCase = true) -> ProcessingStatus.FAILED
                else -> ProcessingStatus.QUEUED
            }
        }
    }
}
