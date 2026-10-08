package com.haptix.app.ui.screens.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.VideoItem
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.data.repository.HapticRepository
import com.haptix.app.data.repository.VideoRepository
import com.haptix.app.domain.model.ProcessingJob
import com.haptix.app.domain.model.ProcessingStatus
import com.haptix.app.domain.model.VideoSource
import com.haptix.app.domain.provider.GeneratedHapticProvider
import com.haptix.app.domain.service.HapticProcessingService
import com.haptix.app.domain.service.RemoteHapticProcessingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI State for YouTube URL Ingestion and Multimodal Processing.
 */
data class YouTubeIngestionUiState(
    val urlInput: String = "",
    val isValidUrl: Boolean = false,
    val extractedVideoId: String? = null,
    val isProcessing: Boolean = false,
    val currentStatus: ProcessingStatus = ProcessingStatus.QUEUED,
    val stageMessage: String = "Enter a YouTube video URL to begin.",
    val progress: Float = 0.0f,
    val activeJobId: String? = null,
    val generatedPattern: HapticPattern? = null,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false
)

class YouTubeIngestionViewModel(
    private val processingService: HapticProcessingService = RemoteHapticProcessingService(),
    private val videoRepository: VideoRepository = DefaultVideoRepository(),
    private val hapticRepository: HapticRepository = DefaultHapticRepository(),
    private val coroutineScope: CoroutineScope? = null,
    private val pollingIntervalMs: Long = 1200L
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(YouTubeIngestionUiState())
    val uiState: StateFlow<YouTubeIngestionUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun onUrlChanged(newUrl: String) {
        val trimmed = newUrl.trim()
        val isValid = isValidYouTubeUrl(trimmed)
        val videoId = if (isValid) extractVideoId(trimmed) else null

        _uiState.value = _uiState.value.copy(
            urlInput = newUrl,
            isValidUrl = isValid,
            extractedVideoId = videoId,
            errorMessage = null
        )
    }

    fun submitUrl() {
        val url = _uiState.value.urlInput.trim()
        if (!isValidYouTubeUrl(url)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a valid YouTube URL (e.g. https://www.youtube.com/watch?v=... or https://youtu.be/...)"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            isCompleted = false,
            errorMessage = null,
            currentStatus = ProcessingStatus.QUEUED,
            stageMessage = "Submitting URL to HaptiX processing pipeline...",
            progress = 0.05f
        )

        scope.launch {
            val submitResult = processingService.submitJob(url)
            if (submitResult.isSuccess) {
                val job = submitResult.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    activeJobId = job.jobId,
                    currentStatus = job.status,
                    stageMessage = job.stageMessage,
                    progress = job.progress.coerceAtLeast(0.10f)
                )
                startPollingStatus(job.jobId)
            } else {
                val err = submitResult.exceptionOrNull()?.message ?: "Failed to submit job"
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    currentStatus = ProcessingStatus.FAILED,
                    errorMessage = err,
                    stageMessage = "Submission failed: $err"
                )
            }
        }
    }

    private fun startPollingStatus(jobId: String) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                if (pollingIntervalMs > 0) {
                    delay(pollingIntervalMs)
                }
                val statusResult = processingService.getJobStatus(jobId)
                if (statusResult.isSuccess) {
                    val job = statusResult.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        currentStatus = job.status,
                        stageMessage = job.stageMessage,
                        progress = job.progress
                    )

                    if (job.status == ProcessingStatus.COMPLETED) {
                        fetchPatternAndFinalize(job)
                        break
                    } else if (job.status == ProcessingStatus.FAILED) {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = job.errorMessage ?: "Processing pipeline reported an error",
                            stageMessage = "Synthesis failed."
                        )
                        break
                    }
                } else {
                    // Check if server is reachable
                    val err = statusResult.exceptionOrNull()?.message ?: "Connection lost"
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Cannot reach HaptiX Processing Service ($err). Ensure local service is running on port 8080."
                    )
                    break
                }
            }
        }
    }

    private suspend fun fetchPatternAndFinalize(job: ProcessingJob) {
        val patternResult = processingService.getGeneratedPattern(job.jobId)
        val pattern = patternResult.getOrNull()

        _uiState.value = _uiState.value.copy(
            isProcessing = false,
            isCompleted = true,
            progress = 1.0f,
            stageMessage = "Multimodal synthesis completed successfully!",
            generatedPattern = pattern
        )
    }

    fun reset() {
        pollingJob?.cancel()
        _uiState.value = YouTubeIngestionUiState()
    }

    companion object {
        fun isValidYouTubeUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val clean = url.trim()
            val hasValidHost = clean.contains("youtube.com") || clean.contains("youtu.be")
            val hasNoBadChars = !clean.contains(";") && !clean.contains("&") || clean.contains("?")
            return hasValidHost && hasNoBadChars && clean.startsWith("http")
        }

        fun extractVideoId(url: String): String {
            return VideoSource.YouTube.extractYouTubeId(url)
        }
    }
}
