package com.haptix.app.domain.model

import java.util.UUID

/**
 * State machine representing progression through the offline/automated multimodal pipeline.
 */
enum class ProcessingStatus {
    QUEUED,
    DOWNLOADING,
    VIDEO_ANALYSIS,
    AUDIO_ANALYSIS,
    FUSION,
    HAPTIC_GENERATION,
    COMPLETED,
    FAILED;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED

    val isRunning: Boolean
        get() = !isTerminal && this != QUEUED
}

/**
 * Encapsulates an asynchronous video-to-haptic synthesis job.
 *
 * Prepared for the future automated workflow (local video or YouTube URL).
 */
data class ProcessingJob(
    val jobId: String = UUID.randomUUID().toString(),
    val videoSource: VideoSource,
    val status: ProcessingStatus = ProcessingStatus.QUEUED,
    val progress: Float = 0.0f,
    val stageMessage: String = "Job queued",
    val errorMessage: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = createdAtMs,
    val resultPattern: HapticPattern? = null
) {
    init {
        require(jobId.isNotBlank()) { "jobId must not be blank" }
        require(progress in 0.0f..1.0f) { "progress must be between 0.0 and 1.0, was $progress" }
        require(createdAtMs >= 0L) { "createdAtMs must be non-negative" }
        require(updatedAtMs >= createdAtMs) { "updatedAtMs ($updatedAtMs) must be >= createdAtMs ($createdAtMs)" }
    }

    fun withProgress(newStatus: ProcessingStatus, newProgress: Float, message: String): ProcessingJob {
        return copy(
            status = newStatus,
            progress = newProgress.coerceIn(0.0f, 1.0f),
            stageMessage = message,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    fun complete(pattern: HapticPattern): ProcessingJob {
        return copy(
            status = ProcessingStatus.COMPLETED,
            progress = 1.0f,
            stageMessage = "Processing completed successfully",
            resultPattern = pattern,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    fun fail(reason: String): ProcessingJob {
        return copy(
            status = ProcessingStatus.FAILED,
            errorMessage = reason,
            stageMessage = "Processing failed: $reason",
            updatedAtMs = System.currentTimeMillis()
        )
    }
}
