# HaptiX: Platform-Independent Cross-Modal Haptic Architecture

This document specifies the platform-independent core architecture for **HaptiX**, decoupling audiovisual analysis, semantic tactile representation, and platform-specific hardware actuation across **Android** and **iOS**.

---

## 1. System Architecture Overview

```
                          ┌───────────────────────────┐
                          │       Video Source        │
                          │ (Local / Curated / Remote)│
                          └─────────────┬─────────────┘
                                        │
                                        ▼
                   ┌─────────────────────────────────────────┐
                   │    Multimodal Analysis Pipeline         │ [FUTURE PHASE 2]
                   │  - Scene Segmentation & Keyframing      │
                   │  - RAFT Optical Flow Motion Analysis    │
                   │  - Visual Semantics (LLaVA-7B Adapter)  │
                   │  - Audio Descriptors & Dialogue Split   │
                   │  - Whisper Tiny Audio Semantics         │
                   │  - Cross-Modal Reliability & Fusion     │
                   └────────────────────┬────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 Platform-Independent Domain Layer [IMPLEMENTED NOW]         │
│                                                                             │
│  HapticPattern                                                              │
│    ├── videoId: String                                                      │
│    ├── source: HapticSource (CURATED | GENERATED | LEGACY_MANUAL)           │
│    └── events: List<HapticEvent>                                            │
│          ├── id: UUID                                                       │
│          ├── startTimeMs, durationMs                                        │
│          ├── semanticType: SemanticHapticType                               │
│          ├── intensity (0.0..1.0), sharpness (0.0..1.0)                    │
│          ├── envelope: HapticEnvelope (Attack -> Sustain -> Release)        │
│          ├── frequencyHz: Float? (optional research calibration)            │
│          ├── confidence: Float (0.0..1.0)                                   │
│          └── evidence: HapticEvidence (traceability & sensor metrics)       │
└───────────────────────┬─────────────────────────────┬───────────────────────┘
                        │                             │
                        ▼                             ▼
         ┌─────────────────────────────┐┌─────────────────────────────┐
         │      Android Adapter        ││         iOS Adapter         │
         │  [AndroidHapticConverter]   ││    [AhapHapticConverter]    │
         │      [IMPLEMENTED NOW]      ││      [IMPLEMENTED NOW]      │
         └──────────────┬──────────────┘└──────────────┬──────────────┘
                        │                              │
                        ▼                              ▼
         ┌─────────────────────────────┐┌─────────────────────────────┐
         │   Android Specific Models   ││   Apple AHAP JSON Schema    │
         │ - HapticFrequencyPattern    ││ - Version 1.0               │
         │ - WaveformProfile           ││ - HapticContinuous /        │
         │ - EnvelopeControlPoints     ││   HapticTransient           │
         │   (API 36+ WaveformBuilder) ││ - Parameter Curves (Attack/ │
         │ - Resonant Fallback (26-35) ││   Release Intensity Control)│
         └──────────────┬──────────────┘└──────────────┬──────────────┘
                        │                              │
                        ▼                              ▼
         ┌─────────────────────────────┐┌─────────────────────────────┐
         │     AndroidHapticPlayer     ││       Core Haptics          │
         │   (Physical Mobile LRA)     ││    (Apple Taptic Engine)    │
         │      [IMPLEMENTED NOW]      ││       [FUTURE PHASE]        │
         └─────────────────────────────┘└─────────────────────────────┘
```

---

## 2. Platform-Independent Domain Models (`com.haptix.app.domain.model`)

The domain layer is 100% pure Kotlin, completely decoupled from Android (`Context`, `VibrationEffect`, `View`, Compose, Media3) and iOS SDK binaries:

### A. `HapticEvent`
The canonical representation of a single tactile stimulus:
- **`id`**: Unique string identifier (UUID).
- **`startTimeMs` & `durationMs`**: Timeline temporal positioning in milliseconds.
- **`semanticType`**: Physical intent category (`SMOOTH`, `SMOOTH_BUILD`, `SMOOTH_RELEASE`, `JUMP`, `LAUNCH`, `HEAVY_IMPACT`, `SOFT_IMPACT`, `RUMBLE`, `TENSION`, `EMOTIONAL_SWELL`, `SWEEP`, `ENERGY`, `METALLIC_CLASH`, `DOUBLE_PULSE`, `RAPID_PULSES`).
- **`intensity` & `sharpness`**: Normalized physical dimensions ($0.0 \dots 1.0$).
- **`attackMs`, `sustainMs`, `releaseMs`**: Attack-Sustain-Release envelope preventing sudden $0 \to \text{MAX} \to 0$ vibration spikes.
- **`frequencyHz`**: Optional experimental target frequency (e.g. 150–240 Hz literature test stimuli).
- **`confidence`**: Cross-modal reliability and synthesis confidence ($0.0 \dots 1.0$).
- **`evidence`**: Multimodal sensor provenance explaining *why* the event was generated.

### B. `HapticEnvelope`
Encapsulates parametric envelope geometry:
$$\text{totalMs} = \text{attackMs} + \text{sustainMs} + \text{releaseMs} \le \text{durationMs}$$
Guarantees physical plausibility and natural tactile acceleration/decay curves.

### C. `HapticEvidence`
Preserves scientific traceability:
- `sceneId`: Scene boundary association.
- `visualEvidence`: Keyframe visual descriptions and characters.
- `motionEvidence`: Optical flow vector dynamics and active region density.
- `audioEvidence`: Sound events, transients, and spectral metrics.
- `frameTimestampsMs` & `audioWindowMs`: Temporal window alignment.
- `visualReliability` & `audioReliability`: Dynamic modal reliability scores.
- `crossModalSimilarity`: Degree of semantic consensus between visual action and auditory cues.

### D. `VideoSource` & `VideoMetadata`
Abstractions representing video inputs:
- `VideoSource.Local`: Bundled raw resources or local filesystem assets.
- `VideoSource.Curated`: Gold-standard reference benchmarks (e.g. F1 onboard telemetry).
- `VideoSource.YouTube`: Remote YouTube streams with automated ID extraction.

### E. `ProcessingJob` & `ProcessingStatus`
State machine preparing for automated background analysis:
$$\text{QUEUED} \to \text{DOWNLOADING} \to \text{VIDEO\_ANALYSIS} \to \text{AUDIO\_ANALYSIS} \to \text{FUSION} \to \text{HAPTIC\_GENERATION} \to \text{COMPLETED} \; (\text{or } \text{FAILED})$$

---

## 3. Decoupled Haptic Source Providers (`com.haptix.app.domain.provider`)

To avoid hardcoded datasets while supporting comparative scientific studies, the repository orchestrates three distinct providers:

```
                          ┌───────────────────────────┐
                          │      HapticRepository     │
                          │ (Configurable Source Mode)│
                          └─────────────┬─────────────┘
                                        │
             ┌──────────────────────────┼──────────────────────────┐
             ▼                          ▼                          ▼
┌─────────────────────────┐┌─────────────────────────┐┌─────────────────────────┐
│  CuratedHapticProvider  ││ GeneratedHapticProvider ││LegacyManualHapticProvider│
│    (F1 / References)    ││   (Pipeline Synthesis)  ││  (Koji 20-Point Manual)  │
│    source = CURATED     ││   source = GENERATED    ││  source = LEGACY_MANUAL  │
└─────────────────────────┘└─────────────────────────┘└─────────────────────────┘
```

1. **`CuratedHapticProvider`**: Loads pre-validated reference tactile stimuli (e.g. F1 onboard curb rumbles, gear shifts).
2. **`GeneratedHapticProvider`**: Loads automated pipeline synthesis outputs (`koji_haptic_representation.json`).
3. **`LegacyManualHapticProvider`**: Preserves the original 20-point manually-authored Koji dataset (`koji_haptics.json`), ensuring zero regressions during verification.

---

## 4. Platform Adapters

### A. Android Converter (`AndroidHapticConverter`)
- Maps domain `HapticPattern` to Android `HapticFrequencyPattern` and `HapticFrequencyPoint`.
- Synthesizes `WaveformProfile` with discrete step discretization for amplitude envelopes.
- Synthesizes `EnvelopeControlPoint` list for Android 16 (API 36+) `WaveformEnvelopeBuilder`.
- Integrates directly with `AndroidHapticPlayer`'s hardware capability tiers (`SUPPORTED`, `LIMITED`, `UNAVAILABLE`).

### B. iOS AHAP Converter (`AhapHapticConverter`)
- Maps domain `HapticPattern` to Apple Haptic Audio Pattern v1.0 JSON format.
- Translates events to `HapticContinuous` and `HapticTransient` blocks with `Time`, `EventDuration`, `HapticIntensity`, and `HapticSharpness`.
- Generates `ParameterCurve` blocks for `HapticIntensityControl` matching the Attack-Sustain-Release envelope.
- **Rule**: Android never consumes AHAP files; this converter exists solely for iOS Core Haptics export.

---

## 5. Implementation Status

| Component | Status | Implementation Details |
| :--- | :--- | :--- |
| **Domain Models** (`VideoMetadata`, `VideoSource`, `Scene`, `ObservationModels`, `HapticEvent`, `HapticPattern`, `HapticEvidence`, `HapticEnvelope`, `ProcessingJob`) | **IMPLEMENTED NOW** | Pure Kotlin domain classes under `com.haptix.app.domain.model` with validation and zero platform dependencies. |
| **Haptic Source Abstraction** (`HapticSourceProvider`, `CuratedHapticProvider`, `GeneratedHapticProvider`, `LegacyManualHapticProvider`) | **IMPLEMENTED NOW** | Under `com.haptix.app.domain.provider`, integrated into `DefaultHapticRepository`. |
| **Android Converter** (`AndroidHapticConverter`) | **IMPLEMENTED NOW** | Converts `HapticPattern` $\to$ Android frequency models and hardware `WaveformProfile`. |
| **iOS AHAP Converter** (`AhapHapticConverter`) | **IMPLEMENTED NOW** | Converts `HapticPattern` $\to$ compliant AHAP JSON string and parameter curves. |
| **Koji Legacy Preservation** | **IMPLEMENTED NOW** | Koji manual dataset loaded via `LegacyManualHapticProvider` with `source = LEGACY_MANUAL`. |
| **UI & Player Preservation** | **IMPLEMENTED NOW** | Jetpack Compose, Obsidian theme, Balthazar font, Media3 ExoPlayer, and `HapticSynchronizer` remain 100% operational. |
| **Unit Test Suite** | **IMPLEMENTED NOW** | 105 tests passing (14 new architectural tests in `PlatformIndependentArchitectureTest`). |
| **Multimodal Analysis Pipeline** | **FUTURE PHASE 2** | RAFT optical flow, scene segmentation, Librosa vocal separation, Whisper Tiny, and multimodal fusion. |
| **Curated F1 Stimuli** | **FUTURE PHASE 3** | Integration of F1 racing telemetry reference patterns. |
| **YouTube URL Downloader** | **FUTURE PHASE 4** | Automated yt-dlp downloading workflow into the pipeline. |
| **Physical iOS CoreHaptics Player** | **FUTURE PHASE 5** | Native Swift / iOS playback client consuming AHAP files. |
