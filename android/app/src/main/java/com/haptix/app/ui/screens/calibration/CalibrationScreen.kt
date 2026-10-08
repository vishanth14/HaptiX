package com.haptix.app.ui.screens.calibration

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.haptics.AndroidHapticPlayer
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.haptics.calibration.CalibrationManager
import com.haptix.app.haptics.calibration.CalibrationStimulus
import com.haptix.app.ui.components.HaptiXBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CalibrationScreen(
    onNavigateBack: () -> Unit = {},
    onPlayTimeline: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val hapticPlayer = remember { AndroidHapticPlayer(context) }
    val calibrationManager = remember { CalibrationManager(context, hapticPlayer) }
    val stimuli = calibrationManager.stimuli

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentStimulus = stimuli[currentIndex]

    var selectedRating by remember { mutableIntStateOf(0) }
    var isSmooth by remember { mutableStateOf(true) }

    val hasVibrator = hapticPlayer.isHapticSupported()
    val hasAmplitude = hapticPlayer.hardwareProfile.hasAmplitudeControl

    val coroutineScope = rememberCoroutineScope()
    val hapticRepo = remember { DefaultHapticRepository(context) }
    val standaloneSynchronizer = remember { HapticSynchronizer(hapticPlayer) }

    var isTimelinePlaying by remember { mutableStateOf(false) }
    var selectedTimelineVideo by remember { mutableStateOf("f1_2025_haptic_trailer") }
    var timelinePosMs by remember { mutableStateOf(0L) }
    var timelineTotalMs by remember { mutableStateOf(130505L) }
    var timelineEventsCount by remember { mutableIntStateOf(24) }
    var timelineActiveEventName by remember { mutableStateOf("IDLE") }

    LaunchedEffect(isTimelinePlaying) {
        while (isTimelinePlaying) {
            delay(30L)
            timelinePosMs += 30L
            standaloneSynchronizer.onTimelineUpdate(timelinePosMs)
            val lastEv = standaloneSynchronizer.getLastDispatched()
            if (lastEv != null && timelinePosMs in lastEv.startTimeMs..(lastEv.startTimeMs + lastEv.durationMs)) {
                timelineActiveEventName = "${lastEv.semanticType.name} (${lastEv.startTimeMs}ms)"
            }
            if (timelinePosMs >= timelineTotalMs) {
                isTimelinePlaying = false
                timelineActiveEventName = "FINISHED"
                standaloneSynchronizer.onPlaybackComplete()
            }
        }
    }

    fun startTimeline(vid: String) {
        coroutineScope.launch {
            selectedTimelineVideo = vid
            timelinePosMs = 0L
            isTimelinePlaying = false
            standaloneSynchronizer.reset()
            val res = hapticRepo.loadHapticPatternForVideo(vid)
            if (res.isSuccess) {
                val pat = res.getOrThrow()
                standaloneSynchronizer.setPattern(pat)
                timelineTotalMs = pat.videoDurationMs ?: pat.events.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 130505L
                timelineEventsCount = pat.events.size
                timelineActiveEventName = "PLAYING"
                isTimelinePlaying = true
            }
        }
    }

    fun triggerManualTimestamp(vid: String, targetMs: Long) {
        coroutineScope.launch {
            val res = hapticRepo.loadHapticPatternForVideo(vid)
            if (res.isSuccess) {
                val pat = res.getOrThrow()
                val ev = pat.events.minByOrNull { kotlin.math.abs(it.startTimeMs - targetMs) }
                if (ev != null) {
                    com.haptix.app.util.HaptiXLog.d("MANUAL_TIMESTAMP_TRIGGER: video=$vid target=${targetMs}ms event=${ev.id} (${ev.semanticType})")
                    hapticPlayer.playEvent(ev)
                }
            }
        }
    }

    HaptiXBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onNavigateBack) {
                    Text("← BACK", color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = "HaptiX Device Calibration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hardware Telemetry Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DEVICE: ${Build.MODEL ?: "Android Device"}", color = Color.LightGray, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text("ANDROID API: ${Build.VERSION.SDK_INT}", color = Color.LightGray, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "VIBRATOR: ${if (hasVibrator) "AVAILABLE" else "UNAVAILABLE"}",
                        color = if (hasVibrator) Color(0xFF00FFCC) else Color.Red,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "AMPLITUDE CONTROL: ${if (hasAmplitude) "SUPPORTED" else "LIMITED"}",
                        color = if (hasAmplitude) Color(0xFF00FFCC) else Color.Yellow,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    val profile = calibrationManager.getActiveProfile()
                    Text(
                        "PERCEPTUAL FLOOR: ${profile.minPerceivableIntensity} (${if (profile.isCalibrated) "CALIBRATED" else "DEFAULT"})",
                        color = Color(0xFF00FFCC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Test Stimulus Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2230)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Test: ${currentStimulus.testId} / ${stimuli.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${currentStimulus.name}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Intensity: ${currentStimulus.intensity}", color = Color.LightGray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("Duration: ${currentStimulus.durationMs} ms", color = Color.LightGray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("Frequency: ${currentStimulus.frequencyHz} Hz", color = Color.LightGray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("Type: ${if (currentStimulus.isContinuous) "CONTINUOUS" else "TRANSIENT"}", color = Color(0xFF88AAFF), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(currentStimulus.description, color = Color.Gray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        calibrationManager.playStimulus(currentStimulus)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text("PLAY", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        calibrationManager.stop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
                ) {
                    Text("STOP", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        if (currentIndex > 0) currentIndex--
                    },
                    enabled = currentIndex > 0
                ) {
                    Text("PREVIOUS", color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        if (currentIndex < stimuli.size - 1) currentIndex++
                    },
                    enabled = currentIndex < stimuli.size - 1
                ) {
                    Text("NEXT", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rating Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How clearly did you feel this? (1 = imperceptible, 5 = very clear)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (r in 1..5) {
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = if (selectedRating == r) 2.dp else 1.dp,
                                        color = if (selectedRating == r) Color(0xFF00FFCC) else Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        if (selectedRating == r) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .clickable {
                                        selectedRating = r
                                        calibrationManager.recordRating(currentStimulus.testId, r, isSmooth)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "$r",
                                    color = if (selectedRating == r) Color(0xFF00FFCC) else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Did the sensation feel smooth?", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        OutlinedButton(
                            onClick = {
                                isSmooth = true
                                calibrationManager.recordRating(currentStimulus.testId, selectedRating, true)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSmooth) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("YES", color = if (isSmooth) Color(0xFF00FFCC) else Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = {
                                isSmooth = false
                                calibrationManager.recordRating(currentStimulus.testId, selectedRating, false)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isSmooth) Color(0xFFFF4444).copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("NO", color = if (!isSmooth) Color(0xFFFF4444) else Color.LightGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // Physical Test Mode Controls (Section 16 & Section 21)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "V4.1 PHYSICAL TEST MODE",
                        color = Color(0xFF00FFCC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Test individual physical sensations immediately on device:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    fun playNamedStimulus(type: String) {
                        val event = when (type) {
                            "REF_TRANSIENT_100" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 100L,
                                intensity = 1.00f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.LIGHT_IMPACT,
                                parameters = mapOf("frequencyHz" to 190.0f, "amplitude" to 1.00f, "classification" to "TRANSIENT")
                            )
                            "REF_TRANSIENT_150" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 150L,
                                intensity = 1.00f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.LIGHT_IMPACT,
                                parameters = mapOf("frequencyHz" to 185.0f, "amplitude" to 1.00f, "classification" to "TRANSIENT")
                            )
                            "REF_TRANSIENT_200" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 200L,
                                intensity = 1.00f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.LIGHT_IMPACT,
                                parameters = mapOf("frequencyHz" to 185.0f, "amplitude" to 1.00f, "classification" to "TRANSIENT")
                            )
                            "REF_TRANSIENT_250" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 250L,
                                intensity = 1.00f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.LIGHT_IMPACT,
                                parameters = mapOf("frequencyHz" to 185.0f, "amplitude" to 1.00f, "classification" to "TRANSIENT")
                            )
                            "STRONG_TRANSIENT" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 150L,
                                intensity = 1.00f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.LIGHT_IMPACT,
                                parameters = mapOf("frequencyHz" to 185.0f, "amplitude" to 1.00f, "classification" to "TRANSIENT")
                            )
                            "STRONG_CONTINUOUS" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 1000L,
                                intensity = 0.80f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.CONTINUOUS_RUMBLE,
                                parameters = mapOf("frequencyHz" to 180.0f, "amplitude" to 0.80f)
                            )
                            "GEAR_SHIFT" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 150L,
                                intensity = 0.95f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.GEAR_SHIFT,
                                parameters = mapOf("frequencyHz" to 205.0f, "amplitude" to 0.95f, "classification" to "TRANSIENT")
                            )
                            "LANDING" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 220L,
                                intensity = 0.95f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.LANDING,
                                parameters = mapOf("frequencyHz" to 185.0f, "amplitude" to 0.95f, "classification" to "TRANSIENT")
                            )
                            "COLLISION" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 260L,
                                intensity = 1.00f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.COLLISION,
                                parameters = mapOf("frequencyHz" to 185.0f, "amplitude" to 1.00f, "classification" to "TRANSIENT")
                            )
                            "SENTINEL_IMPACT" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 280L,
                                intensity = 1.00f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.HEAVY_IMPACT,
                                parameters = mapOf("frequencyHz" to 185.0f, "amplitude" to 1.00f, "classification" to "TRANSIENT")
                            )
                            "ENGINE" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 1500L,
                                intensity = 0.72f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.ENGINE_RUMBLE,
                                parameters = mapOf("frequencyHz" to 135.0f, "amplitude" to 0.72f)
                            )
                            "ACCELERATION" -> com.haptix.app.data.model.HapticEvent(
                                startTimeMs = 0L,
                                durationMs = 2000L,
                                intensity = 0.85f,
                                semanticType = com.haptix.app.data.model.SemanticHapticType.ACCELERATION_RISE,
                                parameters = mapOf("frequencyHz" to 180.0f, "amplitude" to 0.85f)
                            )
                            else -> null
                        }
                        event?.let { calibrationManager.hapticPlayer.playEvent(it) }
                    }

                    // Section: Reference Transients
                    Text(
                        "REFERENCE TRANSIENT SUITE",
                        color = Color(0xFF00FFCC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { playNamedStimulus("REF_TRANSIENT_100") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF193244))
                        ) {
                            Text("REF TRANSIENT 100ms", fontSize = 10.sp, color = Color(0xFF00FFCC), maxLines = 1)
                        }
                        Button(
                            onClick = { playNamedStimulus("REF_TRANSIENT_150") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF193244))
                        ) {
                            Text("REF TRANSIENT 150ms", fontSize = 10.sp, color = Color(0xFF00FFCC), maxLines = 1)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { playNamedStimulus("REF_TRANSIENT_200") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF193244))
                        ) {
                            Text("REF TRANSIENT 200ms", fontSize = 10.sp, color = Color(0xFF00FFCC), maxLines = 1)
                        }
                        Button(
                            onClick = { playNamedStimulus("REF_TRANSIENT_250") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF193244))
                        ) {
                            Text("REF TRANSIENT 250ms", fontSize = 10.sp, color = Color(0xFF00FFCC), maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "PHYSICAL SENSATION TESTS",
                        color = Color(0xFF88AAFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Row 1: Impacts
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { playNamedStimulus("COLLISION") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E1E))
                        ) {
                            Text("COLLISION (1.0)", fontSize = 11.sp, color = Color(0xFFFF6666))
                        }
                        Button(
                            onClick = { playNamedStimulus("SENTINEL_IMPACT") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E1E))
                        ) {
                            Text("SENTINEL IMPACT", fontSize = 11.sp, color = Color(0xFFFF6666))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Mechanical Actions
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { playNamedStimulus("GEAR_SHIFT") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("GEAR SHIFT", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { playNamedStimulus("LANDING") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("LANDING", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: Reference Continuous & Acceleration
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { playNamedStimulus("STRONG_CONTINUOUS") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("STRONG CONTINUOUS", fontSize = 11.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { playNamedStimulus("ACCELERATION") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("ACCELERATION", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 4: Engine Rumble & Generic Strong Transient
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { playNamedStimulus("ENGINE") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("ENGINE RUMBLE", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { playNamedStimulus("STRONG_TRANSIENT") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("STRONG TRANSIENT", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Stop button
                    Button(
                        onClick = { calibrationManager.stop() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
                    ) {
                        Text("STOP IMMEDIATELY", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("FULL TIMELINE PLAYBACK (VIDEO SYNCHRONIZED):", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPlayTimeline("f1_2025_haptic_trailer") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3B5C))
                        ) {
                            Text("PLAY F1 VIDEO", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onPlayTimeline("koji") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3B5C))
                        ) {
                            Text("PLAY KOJI VIDEO", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 18: Manual Timestamp Triggers
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "MANUAL TIMESTAMP TRIGGER (SECTION 18)",
                        color = Color(0xFF00FFCC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Invoke the actual dataset event at exact video timestamps to isolate rendering vs auto-triggering:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("F1 2025 TIMESTAMPS:", color = Color(0xFF88AAFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { triggerManualTimestamp("f1_2025_haptic_trailer", 7100L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("F1 @ 7.1s\n(ACCEL)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF00FFCC))
                        }
                        Button(
                            onClick = { triggerManualTimestamp("f1_2025_haptic_trailer", 27000L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("F1 @ 27.0s\n(CURB)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF00FFCC))
                        }
                        Button(
                            onClick = { triggerManualTimestamp("f1_2025_haptic_trailer", 55100L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("F1 @ 55.1s\n(BRAKE)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF00FFCC))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { triggerManualTimestamp("f1_2025_haptic_trailer", 67200L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("F1 @ 67.2s\n(GEAR)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF00FFCC))
                        }
                        Button(
                            onClick = { triggerManualTimestamp("f1_2025_haptic_trailer", 99100L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E1E))
                        ) {
                            Text("F1 @ 99.1s\n(COLLISION)", fontSize = 10.sp, maxLines = 2, color = Color(0xFFFF6666))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("KOJI TIMESTAMPS:", color = Color(0xFF88AAFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { triggerManualTimestamp("koji", 12200L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("KOJI @ 12.2s\n(JUMP)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF00FFCC))
                        }
                        Button(
                            onClick = { triggerManualTimestamp("koji", 14500L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("KOJI @ 14.5s\n(LAND)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF00FFCC))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { triggerManualTimestamp("koji", 32500L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("KOJI @ 32.5s\n(CLICK)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF00FFCC))
                        }
                        Button(
                            onClick = { triggerManualTimestamp("koji", 39000L) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E1E))
                        ) {
                            Text("KOJI @ 39.0s\n(SENTINEL)", fontSize = 10.sp, maxLines = 2, color = Color(0xFFFF6666))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 17: Play Haptic Timeline Only
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PLAY HAPTIC TIMELINE ONLY (SECTION 17)",
                        color = Color(0xFF00FFCC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Runs local timer through HapticSynchronizer without video to separate sync from timeline actuation:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TIMELINE: ${if (selectedTimelineVideo.contains("f1")) "F1 (24 events)" else "Koji (23 events)"}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "POSITION: ${String.format(java.util.Locale.US, "%.2fs / %.2fs", timelinePosMs / 1000.0, timelineTotalMs / 1000.0)}",
                        color = Color(0xFF00FFCC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ACTIVE EVENT: $timelineActiveEventName",
                        color = Color.Yellow,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DISPATCHED: ${standaloneSynchronizer.getDispatchedCount()} / $timelineEventsCount events",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (timelineTotalMs > 0) (timelinePosMs.toFloat() / timelineTotalMs).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF00FFCC),
                        trackColor = Color(0xFF1E2838)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { startTimeline("f1_2025_haptic_trailer") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E382B))
                        ) {
                            Text("RUN F1 TIMELINE", fontSize = 11.sp, color = Color(0xFF00FFCC))
                        }
                        Button(
                            onClick = { startTimeline("koji") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E382B))
                        ) {
                            Text("RUN KOJI TIMELINE", fontSize = 11.sp, color = Color(0xFF00FFCC))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                isTimelinePlaying = !isTimelinePlaying
                                if (!isTimelinePlaying) {
                                    standaloneSynchronizer.onPause("TIMELINE_TEST_PAUSE")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isTimelinePlaying) "PAUSE" else "RESUME", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                isTimelinePlaying = false
                                timelinePosMs = 0L
                                timelineActiveEventName = "STOPPED"
                                standaloneSynchronizer.reset()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
                        ) {
                            Text("RESET", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 18: Step 10 Reference Stimuli & A/B Device Calibration Test
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "REFERENCE STIMULI & A/B CALIBRATION (STEP 10)",
                        color = Color(0xFF00FFCC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Controlled reference stimuli to evaluate perceptual hierarchy and motor pre-emphasis on device:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("CONTROLLED REFERENCE STIMULI (A - E):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { hapticPlayer.playReferenceStimulus(AndroidHapticPlayer.ReferenceStimulus.STIMULUS_A_LOW_PULSE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A38))
                        ) {
                            Text("A: LOW\n(100ms 0.20)", fontSize = 10.sp, maxLines = 2, color = Color.LightGray)
                        }
                        Button(
                            onClick = { hapticPlayer.playReferenceStimulus(AndroidHapticPlayer.ReferenceStimulus.STIMULUS_B_MEDIUM_PULSE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3845))
                        ) {
                            Text("B: MEDIUM\n(150ms 0.45)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF80D8FF))
                        }
                        Button(
                            onClick = { hapticPlayer.playReferenceStimulus(AndroidHapticPlayer.ReferenceStimulus.STIMULUS_C_STRONG_PULSE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A381E))
                        ) {
                            Text("C: STRONG\n(200ms 0.70)", fontSize = 10.sp, maxLines = 2, color = Color(0xFFA5D6A7))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { hapticPlayer.playReferenceStimulus(AndroidHapticPlayer.ReferenceStimulus.STIMULUS_D_LONG_RUMBLE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF382B1E))
                        ) {
                            Text("D: RUMBLE (800ms 0.50)", fontSize = 10.sp, color = Color(0xFFFFCC80))
                        }
                        Button(
                            onClick = { hapticPlayer.playReferenceStimulus(AndroidHapticPlayer.ReferenceStimulus.STIMULUS_E_CRASH_IMPACT) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1E1E))
                        ) {
                            Text("E: CRASH (180ms 0.95)", fontSize = 10.sp, color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("DIRECT V5 vs V2 CALIBRATED A/B AUDIT:", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Crash Comparison
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                // V5 uncalibrated baseline crash (peak 204)
                                val oldCalib = hapticPlayer.calibration
                                hapticPlayer.calibration = com.haptix.app.haptics.calibration.DeviceHapticCalibration.UNCALIBRATED_PASSTHROUGH
                                hapticPlayer.playEvent(
                                    com.haptix.app.data.model.HapticEvent(
                                        startTimeMs = 105020L, durationMs = 180L, intensity = 0.744f,
                                        semanticType = com.haptix.app.data.model.SemanticHapticType.COLLISION,
                                        attackMs = 40L, sustainMs = 0L, releaseMs = 140L,
                                        parameters = mapOf("description" to "V5 Baseline Crash")
                                    )
                                )
                                hapticPlayer.calibration = oldCalib
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828))
                        ) {
                            Text("V5 CRASH\n(Peak 204)", fontSize = 10.sp, maxLines = 2, color = Color.White)
                        }
                        Button(
                            onClick = {
                                // V2 calibrated dominant crash (peak 255, RMS 172.5)
                                hapticPlayer.playEvent(
                                    com.haptix.app.data.model.HapticEvent(
                                        startTimeMs = 105020L, durationMs = 180L, intensity = 0.760f,
                                        semanticType = com.haptix.app.data.model.SemanticHapticType.COLLISION,
                                        attackMs = 15L, sustainMs = 65L, releaseMs = 100L,
                                        parameters = mapOf("description" to "Landmark Crash Event (Calibrated)")
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A1E1E))
                        ) {
                            Text("V2 CALIBRATED CRASH\n(Peak 255)", fontSize = 10.sp, maxLines = 2, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Climax Beat 1 vs Beat 6
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                hapticPlayer.playEvent(
                                    com.haptix.app.data.model.HapticEvent(
                                        startTimeMs = 123440L, durationMs = 240L, intensity = 0.435f,
                                        semanticType = com.haptix.app.data.model.SemanticHapticType.HEAVY_IMPACT,
                                        attackMs = 20L, sustainMs = 45L, releaseMs = 175L,
                                        parameters = mapOf("description" to "Climax Beat 1 (Building)")
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                        ) {
                            Text("CLIMAX BEAT 1\n(Peak 110)", fontSize = 10.sp, maxLines = 2, color = Color(0xFF80D8FF))
                        }
                        Button(
                            onClick = {
                                hapticPlayer.playEvent(
                                    com.haptix.app.data.model.HapticEvent(
                                        startTimeMs = 125920L, durationMs = 300L, intensity = 0.670f,
                                        semanticType = com.haptix.app.data.model.SemanticHapticType.HEAVY_IMPACT,
                                        attackMs = 20L, sustainMs = 140L, releaseMs = 140L,
                                        parameters = mapOf("description" to "Climax Beat 6 (Final Slam)")
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E38))
                        ) {
                            Text("CLIMAX BEAT 6\n(Peak 255)", fontSize = 10.sp, maxLines = 2, color = Color(0xFFEA80FC), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
