package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticFrequencyPattern
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.util.HaptiXLog
import java.util.Locale

/**
 * Strongly-typed playback synchronization states for gating haptic actuation.
 */
enum class PlaybackSyncState {
    IDLE,
    PLAYING,
    PAUSED,
    BUFFERING,
    SEEKING,
    COMPLETED,
    STOPPED
}

/**
 * Explicit per-event lifecycle states for timeline haptic events to guarantee single-fire
 * actuation per playback pass and accurate skip diagnostics.
 */
enum class EventTriggerState {
    NOT_TRIGGERED,
    ACTIVE,
    COMPLETED;

    companion object {
        @Deprecated("Use ACTIVE instead", ReplaceWith("ACTIVE"))
        val TRIGGERED = ACTIVE
    }
}

/**
 * Coordinates millisecond-accurate, deterministic synchronization between ExoPlayer/Media3
 * video playback timestamps and the research haptic timeline.
 *
 * Authoritative Clock & Synchronization Principles:
 * 1. The VIDEO PLAYER (Media3 currentPosition) is the sole authoritative clock.
 *    No synthetic wall-clock timer or coroutine delay drives haptic event progression.
 * 2. Strict State Gating:
 *    [onTimelineUpdate] only actuates while [syncState] is strictly [PlaybackSyncState.PLAYING].
 *    Ticks received while PAUSED, BUFFERING, STOPPED, or SEEKING never actuate haptics.
 * 3. Strict Temporal Containment Contract:
 *    An event is active IF AND ONLY IF: event.startTimeMs <= (currentPositionMs + actuationLeadMs) < effectiveEndTime.
 *    Outside event intervals, physical actuation is guaranteed to be silent.
 * 4. Single-Fire Dispatch per Pass:
 *    Entering an event interval triggers physical actuation exactly once.
 *    Subsequent synchronization ticks inside the same event window NEVER re-trigger vibration.
 * 5. Discontinuity & Large Forward Jump Handling:
 *    Forward position gaps exceeding [MAX_FORWARD_GAP_MS] (500ms) are treated as discontinuities;
 *    intervening events are marked COMPLETED and skipped without firing.
 * 6. Pause / Buffering Re-arm Policy:
 *    If paused or buffered mid-event, active vibration stops immediately, and the in-progress
 *    event is re-armed so that upon resume within its interval, actuation safely continues.
 * 7. Collision-Free Event Indexing:
 *    Event lifecycle states are tracked by sorted index, preventing collisions even with
 *    duplicate start timestamps or blank event IDs.
 * 8. Configurable Actuation Lead:
 *    Default [actuationLeadMs] is strictly 0L (no hidden offsets). Researchers can calibrate
 *    physical actuator lead without altering raw research timestamps.
 *
 * Threading Model:
 * All state mutations and synchronization calls must execute on the main/player thread context.
 *
 * @param engine The underlying [HapticEngine] responsible for physical actuation.
 * @param minimumImpulseDurationMs Minimum duration window in milliseconds for zero-duration transient impulses.
 * @param actuationLeadMs Configurable lead time in milliseconds (defaults to 0ms).
 */
class HapticSynchronizer(
    private val engine: HapticEngine,
    val minimumImpulseDurationMs: Long = 50L,
    var actuationLeadMs: Long = 0L
) {
    constructor(engine: HapticEngine, toleranceMs: Long) : this(
        engine = engine,
        minimumImpulseDurationMs = toleranceMs,
        actuationLeadMs = 0L
    )

    companion object {
        /**
         * Maximum permissible forward gap between ticks before treating as a seek/discontinuity.
         */
        const val MAX_FORWARD_GAP_MS: Long = 500L
    }

    /**
     * Backward-compatible alias for [minimumImpulseDurationMs].
     */
    @Deprecated("Use minimumImpulseDurationMs for accuracy", ReplaceWith("minimumImpulseDurationMs"))
    val toleranceMs: Long get() = minimumImpulseDurationMs

    var isHapticsEnabled: Boolean = true
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                playbackPassId++
                HaptiXLog.d("[HAPTIX_SYNC] SYNC_GENERATION generation=$playbackPassId")
                if (!value) {
                    isActuating = false
                    engine.stop("HAPTICS_DISABLED_BY_USER")
                    HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=${lastDispatchedEvent?.id ?: "ACTIVE"}")
                } else {
                    alignEventStatesToPosition(lastVideoTimestampMs)
                }
                HaptiXLog.d("[HAPTIX_SYNC] HAPTICS_ENABLED: ${if (value) "true" else "false"}")
            }
        }

    /**
     * Strongly-typed playback synchronization state.
     */
    var syncState: PlaybackSyncState = PlaybackSyncState.PLAYING
        private set

    /**
     * Backward-compatible string representation of playback state.
     */
    var playerState: String
        get() = if (syncState == PlaybackSyncState.STOPPED || syncState == PlaybackSyncState.COMPLETED) "ENDED" else syncState.name
        set(value) {
            syncState = try {
                PlaybackSyncState.valueOf(value.uppercase())
            } catch (_: Exception) {
                when (value.uppercase()) {
                    "ENDED" -> PlaybackSyncState.STOPPED
                    else -> PlaybackSyncState.PLAYING
                }
            }
        }

    private var currentPattern: HapticPattern? = null
    private var currentFrequencyPattern: HapticFrequencyPattern? = null
    private var sortedEventsCache: List<HapticEvent> = emptyList()

    // Collision-free index-based state tracking
    private val dispatchedIndices = mutableSetOf<Int>()
    private val eventStatesByIndex = mutableMapOf<Int, EventTriggerState>()

    private var lastVideoTimestampMs: Long = 0L
    private var lastEvaluatedTimestampMs: Long = -1L
    private var lastDiagnosticLogMs: Long = -1000L
    private var lastDispatchedEvent: HapticEvent? = null
    private var lastDispatchedTimestampMs: Long = 0L
    private var playbackPassId: Long = 1L

    private var isActuating: Boolean = false

    /**
     * Diagnostic query indicating if synchronizer is currently commanding active actuation.
     */
    val isActuatingState: Boolean get() = isActuating

    /**
     * Strongly-typed playback generation counter incremented on any state invalidation
     * (seek, pause, buffering, video completion, reset, haptics toggle).
     * Any asynchronous haptic task must capture this generation and cancel execution
     * if the captured generation does not match the active generation.
     */
    val playbackGeneration: Long get() = playbackPassId

    /**
     * Returns true if the provided [generation] token matches the active [playbackGeneration].
     */
    fun isGenerationValid(generation: Long): Boolean = generation == playbackPassId

    /**
     * Executes [block] only if [generation] matches the current active generation.
     * Logs SYNC_STALE_CALLBACK and drops execution if the generation is stale.
     */
    fun executeIfGenerationValid(generation: Long, block: () -> Unit): Boolean {
        return if (generation == playbackPassId) {
            block()
            true
        } else {
            HaptiXLog.d("[HAPTIX_SYNC] SYNC_STALE_CALLBACK ignored generation=$generation active=$playbackPassId")
            false
        }
    }

    /**
     * Returns the current playback pass/generation identifier.
     */
    fun getPlaybackPassId(): Long = playbackPassId

    /**
     * Associates a [HapticFrequencyPattern] with the active video playback session.
     */
    fun setFrequencyPattern(pattern: HapticFrequencyPattern) {
        this.currentFrequencyPattern = pattern
        setPattern(pattern.toHapticPattern())
    }

    /**
     * Returns the currently loaded [HapticFrequencyPattern], if any.
     */
    fun getFrequencyPattern(): HapticFrequencyPattern? = currentFrequencyPattern

    /**
     * Associates a [HapticPattern] with the active video playback session.
     * Sanitizes and orders events deterministically.
     */
    fun setPattern(pattern: HapticPattern) {
        this.currentPattern = pattern
        // Reject malformed events safely (negative timestamps or negative durations)
        this.sortedEventsCache = pattern.events
            .filter { event ->
                val isValid = event.startTimeMs >= 0L && event.durationMs >= 0L
                if (!isValid) {
                    HaptiXLog.w("[HAPTIX_SYNC] REJECT_MALFORMED_EVENT id=${event.id} start=${event.startTimeMs} dur=${event.durationMs}")
                }
                isValid
            }
            .sortedBy { it.startTimeMs }

        engine.loadPattern(pattern)
        resetTimelineState()
        syncState = PlaybackSyncState.PLAYING
    }

    /**
     * Returns the currently loaded [HapticPattern], if any.
     */
    fun getPattern(): HapticPattern? = currentPattern

    /**
     * Identifies pending haptic events matching the given video playback timestamp.
     */
    fun getEventsAtTimestamp(videoPositionMs: Long): List<HapticEvent> {
        val syncPos = (videoPositionMs + actuationLeadMs).coerceAtLeast(0L)
        return sortedEventsCache.filter { event ->
            val end = getEffectiveEndTime(event)
            syncPos in event.startTimeMs until end
        }
    }

    /**
     * Computes the effective end time for an event, guaranteeing an actuation window
     * even for zero-duration transient impulses.
     */
    private fun getEffectiveEndTime(event: HapticEvent): Long {
        return if (event.durationMs > 0L) {
            event.startTimeMs + event.durationMs
        } else {
            event.startTimeMs + minimumImpulseDurationMs.coerceAtLeast(20L)
        }
    }

    /**
     * Invoked when Media3 ExoPlayer emits authoritative millisecond playback updates.
     *
     * Core Invariants Enforced:
     * 1. Gated by [syncState] == PLAYING. Ticks while PAUSED, BUFFERING, STOPPED are ignored.
     * 2. Large forward jumps (> MAX_FORWARD_GAP_MS) skip intervening events without playing.
     * 3. Backward leaps auto-trigger seek resynchronization for the new playback pass.
     * 4. Strict interval containment: event.startTimeMs <= syncPosition < effectiveEndTime.
     * 5. Events trigger ONCE per pass when entered; repeated ticks do NOT retrigger.
     * 6. Silence outside active event intervals.
     *
     * @param currentPositionMs Current video playback progress in milliseconds.
     */
    fun onTimelineUpdate(currentPositionMs: Long) {
        // Gating: Must never actuate when not actively playing
        if (syncState != PlaybackSyncState.PLAYING) {
            return
        }

        val safePositionMs = currentPositionMs.coerceAtLeast(0L)

        if (sortedEventsCache.isEmpty()) {
            if (isActuating) {
                val stoppedId = lastDispatchedEvent?.id ?: "UNKNOWN"
                engine.stop("NO_EVENTS")
                isActuating = false
                HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=$stoppedId")
            }
            this.lastVideoTimestampMs = safePositionMs
            return
        }

        if (!isHapticsEnabled) {
            if (isActuating) {
                val stoppedId = lastDispatchedEvent?.id ?: "UNKNOWN"
                engine.stop("HAPTICS_DISABLED")
                isActuating = false
                HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=$stoppedId")
            }
            this.lastVideoTimestampMs = safePositionMs
            return
        }

        val delta = safePositionMs - lastVideoTimestampMs

        // 1. Large Forward Discontinuity Detection (> MAX_FORWARD_GAP_MS)
        if (lastVideoTimestampMs > 0L && delta > MAX_FORWARD_GAP_MS) {
            HaptiXLog.d("[HAPTIX_SYNC] Forward discontinuity detected: ${lastVideoTimestampMs}ms -> ${safePositionMs}ms (delta=${delta}ms > ${MAX_FORWARD_GAP_MS}ms)")
            onSeek(safePositionMs)
        }

        // 2. Backward seek or video loop auto-detection (jump backward >= 150ms without explicit onSeek)
        if (lastVideoTimestampMs > 0L && safePositionMs < lastVideoTimestampMs - 150L) {
            HaptiXLog.d("[HAPTIX_SYNC] Backward jump detected: ${lastVideoTimestampMs}ms -> ${safePositionMs}ms")
            onSeek(safePositionMs)
            return
        }

        // Minor clock jitter or frame drop (< 150ms backward) - ignore
        if (safePositionMs < lastVideoTimestampMs) {
            return
        }

        // Stationary video detection (e.g. paused, buffering, or identical tick)
        if (safePositionMs == lastEvaluatedTimestampMs && lastEvaluatedTimestampMs >= 0L) {
            return
        }
        lastEvaluatedTimestampMs = safePositionMs

        val syncPositionMs = (safePositionMs + actuationLeadMs).coerceAtLeast(0L)

        // 3. Strict Temporal Containment: Only events where start <= syncPos < end are active
        val activeIndices = mutableListOf<Int>()
        for (i in sortedEventsCache.indices) {
            val event = sortedEventsCache[i]
            val end = getEffectiveEndTime(event)
            if (syncPositionMs >= event.startTimeMs && syncPositionMs < end) {
                activeIndices.add(i)
            }
        }

        // 4. Identify un-triggered events whose onset was crossed during forward progression
        val isForwardProgression = lastVideoTimestampMs in 0 until syncPositionMs
        if (isForwardProgression) {
            for (i in sortedEventsCache.indices) {
                val event = sortedEventsCache[i]
                val state = eventStatesByIndex[i] ?: EventTriggerState.NOT_TRIGGERED

                if (state == EventTriggerState.NOT_TRIGGERED &&
                    lastVideoTimestampMs < event.startTimeMs && syncPositionMs >= event.startTimeMs
                ) {
                    dispatchedIndices.add(i)
                    lastDispatchedEvent = event
                    lastDispatchedTimestampMs = syncPositionMs

                    val end = getEffectiveEndTime(event)
                    if (syncPositionMs < end) {
                        eventStatesByIndex[i] = EventTriggerState.ACTIVE
                        logStructuredSync(syncPositionMs, i, event, "TRIGGER")
                        HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_START id=${event.id} position=${syncPositionMs}ms")
                        engine.playEvent(event)
                        isActuating = true
                    } else {
                        // Position passed event end during this tick: trigger transient impulse and mark completed
                        eventStatesByIndex[i] = EventTriggerState.COMPLETED
                        logStructuredSync(syncPositionMs, i, event, "TRIGGER_STEP")
                        HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_START id=${event.id} position=${syncPositionMs}ms")
                        engine.playEvent(event)
                    }
                }
            }
        }

        // 5. Actuation Logic: Trigger un-actuated active events exactly once
        for (i in activeIndices) {
            val state = eventStatesByIndex[i] ?: EventTriggerState.NOT_TRIGGERED

            if (state == EventTriggerState.NOT_TRIGGERED) {
                eventStatesByIndex[i] = EventTriggerState.ACTIVE
                dispatchedIndices.add(i)
                val event = sortedEventsCache[i]
                lastDispatchedEvent = event
                lastDispatchedTimestampMs = syncPositionMs

                logStructuredSync(
                    positionMs = syncPositionMs,
                    eventIndex = i,
                    event = event,
                    action = "TRIGGER"
                )
                HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_START id=${event.id} position=${syncPositionMs}ms")
                engine.playEvent(event)
                isActuating = true
            }
            // If already ACTIVE: position remains inside interval -> DO NOT RETRIGGER
        }

        // 6. Mark past and future event states relative to current position
        for (i in sortedEventsCache.indices) {
            val event = sortedEventsCache[i]
            val end = getEffectiveEndTime(event)

            if (syncPositionMs >= end) {
                eventStatesByIndex[i] = EventTriggerState.COMPLETED
                dispatchedIndices.add(i)
            } else if (syncPositionMs < event.startTimeMs) {
                eventStatesByIndex[i] = EventTriggerState.NOT_TRIGGERED
                dispatchedIndices.remove(i)
            }
        }

        // 7. Outside all active event intervals: engine MUST be silent
        if (activeIndices.isEmpty()) {
            if (isActuating) {
                val stoppedId = lastDispatchedEvent?.id ?: "UNKNOWN"
                engine.stop("SILENCE_OUTSIDE_EVENT_INTERVAL")
                isActuating = false
                HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=$stoppedId")
            }
        }

        // 8. Periodic diagnostic telemetry (~250ms)
        if (kotlin.math.abs(safePositionMs - lastDiagnosticLogMs) >= 250L) {
            lastDiagnosticLogMs = safePositionMs
            val activeIdsStr = if (activeIndices.isEmpty()) "NONE" else activeIndices.joinToString(",") { sortedEventsCache[it].id }
            val hapticStateStr = if (isActuating) "PLAYING" else "SILENCE"
            val syncStateStr = getSyncState()
            val firstActive = activeIndices.firstOrNull()?.let { sortedEventsCache[it] }
            val startStr = firstActive?.startTimeMs?.let { String.format(Locale.US, "%.2fs", it / 1000.0) } ?: "NONE"
            val endStr = firstActive?.let { String.format(Locale.US, "%.2fs", getEffectiveEndTime(it) / 1000.0) } ?: "NONE"
            val eventTypeStr = firstActive?.semanticType?.name ?: "NONE"
            val remainingMs = firstActive?.let { getEffectiveEndTime(it) - syncPositionMs } ?: 0L

            HaptiXLog.d(
                """
                [HAPTIX_SYNC_DIAGNOSTIC]
                position=${String.format(Locale.US, "%.2f", safePositionMs / 1000.0)}s
                activeEvent=$activeIdsStr
                eventType=$eventTypeStr
                eventWindow=$startStr-$endStr
                remaining=${remainingMs}ms
                hapticState=$hapticStateStr
                syncState=$syncStateStr
                pass=$playbackPassId
                lead=${actuationLeadMs}ms
                """.trimIndent()
            )
        }

        this.lastVideoTimestampMs = safePositionMs
    }

    /**
     * Aligns event states strictly relative to the authoritative video playback position.
     * Events completely preceding [positionMs] are marked COMPLETED (dispatched / skipped).
     * Events at or after [positionMs] are reset to NOT_TRIGGERED and eligible to actuate.
     */
    fun alignEventStatesToPosition(positionMs: Long) {
        val cleanPosition = positionMs.coerceAtLeast(0L)
        val syncPos = (cleanPosition + actuationLeadMs).coerceAtLeast(0L)
        dispatchedIndices.clear()
        for (i in sortedEventsCache.indices) {
            val event = sortedEventsCache[i]
            val end = getEffectiveEndTime(event)
            if (syncPos >= end) {
                eventStatesByIndex[i] = EventTriggerState.COMPLETED
                dispatchedIndices.add(i)
            } else {
                eventStatesByIndex[i] = EventTriggerState.NOT_TRIGGERED
            }
        }
    }

    /**
     * Handles seek operations within ExoPlayer.
     * Immediately terminates active vibration, invalidates previous playback generations,
     * and resets dispatch eligibility relative to the new target timestamp.
     *
     * Invariants:
     * 1. Active vibration is immediately cancelled.
     * 2. Playback generation is incremented to invalidate stale asynchronous work.
     * 3. Events prior to [seekPositionMs] are marked COMPLETED and skipped without firing.
     * 4. Events at or after [seekPositionMs] are marked NOT_TRIGGERED and eligible to fire.
     *
     * @param seekPositionMs The target timeline position in milliseconds.
     */
    fun onSeek(seekPositionMs: Long) {
        val cleanPosition = seekPositionMs.coerceAtLeast(0L)
        val oldPosSec = String.format(Locale.US, "%.2f", lastVideoTimestampMs / 1000.0)
        val newPosSec = String.format(Locale.US, "%.2f", cleanPosition / 1000.0)

        playbackPassId++
        val stoppedId = lastDispatchedEvent?.id ?: "PRE_SEEK"
        engine.stop("SEEK_TO_${cleanPosition}MS")
        isActuating = false

        if (cleanPosition < lastVideoTimestampMs) {
            HaptiXLog.d("[HAPTIX_SYNC] SYNC_REWIND from=${lastVideoTimestampMs}ms to=${cleanPosition}ms")
        } else if (cleanPosition > lastVideoTimestampMs) {
            HaptiXLog.d("[HAPTIX_SYNC] SYNC_FAST_FORWARD from=${lastVideoTimestampMs}ms to=${cleanPosition}ms")
        }
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_SEEK from=${lastVideoTimestampMs}ms to=${cleanPosition}ms")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_GENERATION generation=$playbackPassId")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=$stoppedId")
        HaptiXLog.d("[HAPTIX_SYNC] SEEK old=$oldPosSec new=$newPosSec action=RESET_EVENT_STATE pass=$playbackPassId")

        lastVideoTimestampMs = cleanPosition
        lastEvaluatedTimestampMs = -1L
        lastDiagnosticLogMs = -1000L

        alignEventStatesToPosition(cleanPosition)
    }

    /**
     * Pauses active haptic playback (e.g. when video pauses or user pauses).
     * Stops active actuation immediately, increments playback generation to drop stale tasks,
     * and re-arms in-progress events so that upon resuming, actuation safely continues if still inside the event interval.
     *
     * @param reason Diagnostic explanation for the pause.
     * @param positionMs Optional authoritative video timestamp when pause occurred.
     */
    fun onPause(reason: String = "VIDEO_PAUSE", positionMs: Long? = null) {
        syncState = PlaybackSyncState.PAUSED
        isActuating = false
        playbackPassId++
        val stoppedId = lastDispatchedEvent?.id ?: "ACTIVE"
        engine.stop(reason)

        val pos = positionMs?.coerceAtLeast(0L) ?: lastVideoTimestampMs
        lastVideoTimestampMs = pos

        // Re-arm any active event currently in progress so it can resume upon play
        for (i in sortedEventsCache.indices) {
            val event = sortedEventsCache[i]
            val end = getEffectiveEndTime(event)
            if (pos in event.startTimeMs until end) {
                if (eventStatesByIndex[i] == EventTriggerState.ACTIVE) {
                    eventStatesByIndex[i] = EventTriggerState.NOT_TRIGGERED
                    dispatchedIndices.remove(i)
                    HaptiXLog.d("[HAPTIX_SYNC] Re-armed active event #${i} (${event.id}) across pause at ${pos}ms")
                }
            }
        }

        val posSec = String.format(Locale.US, "%.2f", pos / 1000.0)
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_PAUSE position=${pos}ms")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=$stoppedId")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_GENERATION generation=$playbackPassId")
        HaptiXLog.d("[HAPTIX_SYNC] PAUSE position=$posSec action=CANCEL_ACTIVE_HAPTIC")
    }

    /**
     * Handles media player buffering state.
     * Cancels active vibration safely, halts haptic timeline advancement, increments generation,
     * and re-arms in-progress events for resume.
     *
     * @param positionMs Optional authoritative video timestamp when buffering began.
     */
    fun onBuffering(positionMs: Long? = null) {
        syncState = PlaybackSyncState.BUFFERING
        isActuating = false
        playbackPassId++
        val stoppedId = lastDispatchedEvent?.id ?: "ACTIVE"
        engine.stop("VIDEO_BUFFERING")

        val pos = positionMs?.coerceAtLeast(0L) ?: lastVideoTimestampMs
        lastVideoTimestampMs = pos

        // Re-arm any active event currently in progress
        for (i in sortedEventsCache.indices) {
            val event = sortedEventsCache[i]
            val end = getEffectiveEndTime(event)
            if (pos in event.startTimeMs until end) {
                if (eventStatesByIndex[i] == EventTriggerState.ACTIVE) {
                    eventStatesByIndex[i] = EventTriggerState.NOT_TRIGGERED
                    dispatchedIndices.remove(i)
                    HaptiXLog.d("[HAPTIX_SYNC] Re-armed active event #${i} (${event.id}) across buffering at ${pos}ms")
                }
            }
        }

        val posSec = String.format(Locale.US, "%.2f", pos / 1000.0)
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_BUFFER position=${pos}ms")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=$stoppedId")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_GENERATION generation=$playbackPassId")
        HaptiXLog.d("[HAPTIX_SYNC] BUFFERING position=$posSec action=CANCEL_ACTIVE_HAPTIC")
    }

    /**
     * Resumes haptic synchronization from the actual Media3 currentPosition.
     * Authoritatively re-aligns event states so that any event completely preceding
     * [currentPositionMs] is marked COMPLETED, and upcoming or active events are eligible.
     */
    fun onResume(currentPositionMs: Long) {
        syncState = PlaybackSyncState.PLAYING
        val cleanPos = currentPositionMs.coerceAtLeast(0L)
        lastVideoTimestampMs = cleanPos
        lastEvaluatedTimestampMs = -1L

        val syncPos = (cleanPos + actuationLeadMs).coerceAtLeast(0L)
        for (i in sortedEventsCache.indices) {
            val event = sortedEventsCache[i]
            val end = getEffectiveEndTime(event)
            if (syncPos >= end) {
                eventStatesByIndex[i] = EventTriggerState.COMPLETED
                dispatchedIndices.add(i)
            } else if (eventStatesByIndex[i] != EventTriggerState.ACTIVE) {
                eventStatesByIndex[i] = EventTriggerState.NOT_TRIGGERED
                dispatchedIndices.remove(i)
            }
        }

        engine.start()
        val posSec = String.format(Locale.US, "%.2f", cleanPos / 1000.0)
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_RESUME position=${cleanPos}ms")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_PLAY position=${cleanPos}ms")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_GENERATION generation=$playbackPassId")
        HaptiXLog.d("[HAPTIX_SYNC] RESUME position=$posSec action=RESUME_SYNC")
    }

    /**
     * Called when video playback reaches the end of the media stream.
     */
    fun onPlaybackComplete() {
        syncState = PlaybackSyncState.STOPPED
        isActuating = false
        playbackPassId++
        val stoppedId = lastDispatchedEvent?.id ?: "FINAL"
        engine.stop("PLAYBACK_COMPLETE")
        val posSec = String.format(Locale.US, "%.2f", lastVideoTimestampMs / 1000.0)
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_EVENT_STOP id=$stoppedId")
        HaptiXLog.d("[HAPTIX_SYNC] SYNC_GENERATION generation=$playbackPassId")
        HaptiXLog.d("[HAPTIX_SYNC] COMPLETE position=$posSec action=STOP")
    }

    /**
     * Resets synchronizer state, typically on video stop or new stimulus load.
     */
    fun reset() {
        syncState = PlaybackSyncState.STOPPED
        isActuating = false
        playbackPassId++
        engine.stop("TIMELINE_RESET")
        resetTimelineState()
    }

    private fun resetTimelineState() {
        playbackPassId++
        lastVideoTimestampMs = 0L
        lastEvaluatedTimestampMs = -1L
        lastDiagnosticLogMs = -1000L
        dispatchedIndices.clear()
        eventStatesByIndex.clear()
        for (i in sortedEventsCache.indices) {
            eventStatesByIndex[i] = EventTriggerState.NOT_TRIGGERED
        }
        lastDispatchedEvent = null
        lastDispatchedTimestampMs = 0L
    }

    /**
     * Returns total count of dispatched events in the current playback cycle.
     */
    fun getDispatchedCount(): Int = dispatchedIndices.size

    /**
     * Returns the most recently dispatched event for debug instrumentation.
     */
    fun getLastDispatched(): HapticEvent? = lastDispatchedEvent

    /**
     * Returns the video position timestamp when the most recent event was dispatched.
     */
    fun getLastDispatchedTimestampMs(): Long = lastDispatchedTimestampMs

    /**
     * Returns the current state of a given event by eventId.
     */
    fun getEventState(eventId: String): EventTriggerState {
        val index = sortedEventsCache.indexOfFirst { it.id == eventId }
        return if (index >= 0) eventStatesByIndex[index] ?: EventTriggerState.NOT_TRIGGERED
        else EventTriggerState.NOT_TRIGGERED
    }

    /**
     * Returns the current state of a given event by sorted index.
     */
    fun getEventStateByIndex(index: Int): EventTriggerState {
        return eventStatesByIndex[index] ?: EventTriggerState.NOT_TRIGGERED
    }

    /**
     * Returns the index of a given event in the sorted timeline cache, or -1 if not found.
     */
    fun getEventIndex(event: HapticEvent): Int = sortedEventsCache.indexOfFirst { it.id == event.id }

    /**
     * Returns all events that are currently active at the given video position.
     */
    fun getActiveEvents(videoPositionMs: Long): List<HapticEvent> {
        val syncPos = (videoPositionMs + actuationLeadMs).coerceAtLeast(0L)
        return sortedEventsCache.filter { event ->
            val end = getEffectiveEndTime(event)
            syncPos in event.startTimeMs until end
        }
    }

    /**
     * Returns the next upcoming event relative to the given video position, or null if none remain.
     */
    fun getNextEvent(videoPositionMs: Long): HapticEvent? {
        val syncPos = (videoPositionMs + actuationLeadMs).coerceAtLeast(0L)
        return sortedEventsCache.firstOrNull { it.startTimeMs > syncPos }
    }

    /**
     * Truthful synchronization state query without fabricated precision metrics.
     */
    fun getSyncState(): String {
        return when {
            !isHapticsEnabled -> "HAPTICS OFF"
            syncState == PlaybackSyncState.BUFFERING -> "BUFFERING"
            syncState == PlaybackSyncState.PAUSED -> "PAUSED"
            syncState == PlaybackSyncState.SEEKING -> "SEEKING"
            syncState == PlaybackSyncState.STOPPED -> "STOPPED"
            isActuating -> "LOCKED"
            else -> "SYNCED"
        }
    }

    private fun logStructuredSync(
        positionMs: Long,
        eventIndex: Int,
        event: HapticEvent,
        action: String
    ) {
        val posSec = String.format(Locale.US, "%.2f", positionMs / 1000.0)
        val startSec = String.format(Locale.US, "%.2f", event.startTimeMs / 1000.0)
        val endSec = String.format(Locale.US, "%.2f", getEffectiveEndTime(event) / 1000.0)

        HaptiXLog.d("[HAPTIX_SYNC] position=$posSec index=$eventIndex event=${event.id} eventStart=$startSec eventEnd=$endSec action=$action pass=$playbackPassId lead=${actuationLeadMs}ms")

        val posSec3 = String.format(Locale.US, "%.3fs", positionMs / 1000.0)
        val startSec3 = String.format(Locale.US, "%.3fs", event.startTimeMs / 1000.0)
        val endSec3 = String.format(Locale.US, "%.3fs", getEffectiveEndTime(event) / 1000.0)
        val videoId = currentPattern?.videoId ?: "UNKNOWN"
        val freqStr = event.frequencyHz?.let { String.format(Locale.US, "%.1fHz", it) }
            ?: (event.parameters["frequencyHz"] as? Number)?.let { String.format(Locale.US, "%.1fHz", it.toFloat()) }
            ?: "N/A"
        val androidRep = (engine as? AndroidHapticPlayer)?.lastDispatchedRepresentation ?: "VibrationEffect"

        HaptiXLog.d(
            """
            [HAPTIX-HAPTIC]
            video=$videoId
            position=$posSec3
            eventIndex=$eventIndex
            eventStart=$startSec3
            eventEnd=$endSec3
            sourceFrequency=$freqStr
            sourceAmplitude=${String.format(Locale.US, "%.3f", event.intensity)}
            sourceDuration=${event.durationMs}ms
            androidRepresentation=$androidRep
            triggered=true
            """.trimIndent()
        )
    }
}
