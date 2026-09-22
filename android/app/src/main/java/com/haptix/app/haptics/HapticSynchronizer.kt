package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern

/**
 * Coordinates millisecond-accurate synchronization between ExoPlayer video playback timestamps
 * and the research frequency haptic timeline.
 *
 * Synchronization Contract:
 * Given a video playback timestamp `T_video` (in milliseconds), a haptic event with
 * start timestamp `T_event` is scheduled or triggered when `T_video` enters the activation
 * window `[T_event, T_event + toleranceMs]` or crosses `T_event` during forward progression.
 *
 * Seeking & Deduplication Contract:
 * - Forward seeking skips past events without spurious actuation.
 * - Backward seeking restores playback eligibility for upcoming events.
 * - Deduplication guarantees each event fires at most once per playback pass.
 * - Active vibration is immediately canceled on pause, seek, reset, or video completion.
 *
 * @param engine The underlying [HapticEngine] responsible for physical actuation.
 * @param toleranceMs Permissible jitter window in milliseconds for event alignment.
 */
class HapticSynchronizer(
    private val engine: HapticEngine,
    val toleranceMs: Long = 50L
) {
    var isHapticsEnabled: Boolean = true
    private var currentPattern: HapticPattern? = null
    private var sortedEventsCache: List<HapticEvent> = emptyList()
    private val dispatchedIndices = mutableSetOf<Int>()
    private var lastVideoTimestampMs: Long = 0L
    private var lastDispatchedEvent: HapticEvent? = null

    /**
     * Associates a [HapticPattern] with the active video playback session.
     */
    fun setPattern(pattern: HapticPattern) {
        this.currentPattern = pattern
        this.sortedEventsCache = pattern.sortedEvents()
        engine.loadPattern(pattern)
        reset()
    }

    /**
     * Returns the currently loaded [HapticPattern], if any.
     */
    fun getPattern(): HapticPattern? = currentPattern

    /**
     * Identifies pending haptic events matching the given video playback timestamp.
     */
    fun getEventsAtTimestamp(videoPositionMs: Long): List<HapticEvent> {
        val pattern = currentPattern ?: return emptyList()
        return pattern.events.filter { event ->
            videoPositionMs in event.startTimeMs..(event.startTimeMs + toleranceMs)
        }
    }

    /**
     * Invoked when ExoPlayer emits millisecond-accurate playback timestamp updates.
     *
     * @param currentPositionMs Current video playback progress in milliseconds.
     */
    fun onTimelineUpdate(currentPositionMs: Long) {
        if (!isHapticsEnabled || sortedEventsCache.isEmpty()) {
            this.lastVideoTimestampMs = currentPositionMs
            return
        }

        // Detect unexpected large backward jump without an explicit onSeek call
        if (currentPositionMs < lastVideoTimestampMs - toleranceMs) {
            onSeek(currentPositionMs)
            return
        }

        for (index in sortedEventsCache.indices) {
            if (index in dispatchedIndices) continue

            val event = sortedEventsCache[index]
            val isInWindow = currentPositionMs in event.startTimeMs..(event.startTimeMs + toleranceMs)
            val crossedInStep = lastVideoTimestampMs in 0 until event.startTimeMs && currentPositionMs >= event.startTimeMs

            if (isInWindow || crossedInStep) {
                dispatchedIndices.add(index)
                lastDispatchedEvent = event
                engine.playEvent(event)
            }
        }

        this.lastVideoTimestampMs = currentPositionMs
    }

    /**
     * Handles seek operations within ExoPlayer.
     * Immediately terminates active vibration and resets dispatch eligibility
     * relative to the new target timestamp.
     *
     * @param seekPositionMs The target timeline position in milliseconds.
     */
    fun onSeek(seekPositionMs: Long) {
        engine.stop()
        lastVideoTimestampMs = seekPositionMs.coerceAtLeast(0L)

        dispatchedIndices.clear()
        // Mark all events that strictly precede the new position as already dispatched
        for (index in sortedEventsCache.indices) {
            val event = sortedEventsCache[index]
            if (event.startTimeMs < seekPositionMs) {
                dispatchedIndices.add(index)
            }
        }
    }

    /**
     * Pauses active haptic playback (e.g. when video pauses).
     */
    fun onPause() {
        engine.stop()
    }

    /**
     * Resumes haptic synchronization at the current timestamp.
     */
    fun onResume(currentPositionMs: Long) {
        lastVideoTimestampMs = currentPositionMs
        engine.start()
    }

    /**
     * Called when video playback reaches the end of the media stream.
     */
    fun onPlaybackComplete() {
        engine.stop()
    }

    /**
     * Resets synchronizer state, typically on video stop or completion.
     */
    fun reset() {
        engine.stop()
        lastVideoTimestampMs = 0L
        dispatchedIndices.clear()
        lastDispatchedEvent = null
    }

    /**
     * Returns total count of dispatched events in the current playback cycle.
     */
    fun getDispatchedCount(): Int = dispatchedIndices.size

    /**
     * Returns the most recently dispatched event for debug instrumentation.
     */
    fun getLastDispatched(): HapticEvent? = lastDispatchedEvent
}
