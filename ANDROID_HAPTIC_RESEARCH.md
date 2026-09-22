# HaptiX Android: Frequency-Based Haptic Architecture & Research Documentation

## 1. Executive Summary & Research Motivation
HaptiX is a scientific research platform designed to evaluate human perceptual responses to synchronized multimodal stimuli (audiovisual content coupled with tactile actuation). On Android, standard tactile feedback has historically been limited to coarse rumbling or basic on/off pulses. Modern vibrotactile research demonstrates that human mechanoreceptors perceive distinct textural qualities when stimuli modulate in frequency and amplitude over time.

This document details the engineering specifications, public sources, actuator physics, Android OS limitations, synchronization mechanisms, and fallback strategies implemented in HaptiX Android.

---

## 2. Online & Literature Sources Consulted

### Primary Public & Platform Documentation
1. **Android Developers API Reference (API Level 36 / Android 16)**
   - `android.os.Vibrator.areEnvelopeEffectsSupported()`: Capability query determining whether the hardware abstraction layer (HAL) and actuator support envelope effects.
   - `android.os.VibrationEffect.WaveformEnvelopeBuilder`: Control point synthesis (`addControlPoint(float amplitude, float frequencyHz, long durationMillis)`).
   - `android.os.Vibrator.getFrequencyProfile()`: Accessor for `VibratorFrequencyProfile` defining output acceleration over frequency.
   - `android.os.Vibrator.getResonantFrequency()`: Actuator mechanical resonance query.
   - URL: https://developer.android.com/reference/android/os/Vibrator
   - URL: https://developer.android.com/reference/android/os/VibrationEffect.WaveformEnvelopeBuilder

2. **Android Open Source Project (AOSP) Haptics Architecture**
   - HAL Vibrator Service specification (IVibrator AIDL).
   - Linear Resonant Actuator (LRA) calibration guidelines: Band-pass tuning, resonant frequency tracking, acceleration thresholds.
   - Target vibration frequency rules: common implementations target $1/2$ to $2/3$ of resonant frequency $f_0$, extending to $f_0$.
   - URL: https://source.android.com/devices/sensors/haptics

3. **Peer-Reviewed Vibrotactile Psychophysics & Actuator Engineering**
   - Choi, S., & Kuchenbecker, K. J. (2013). *Vibrotactile Display: Perception, Technology, and Applications*. Proceedings of the IEEE, 101(9), 2093–2104.
   - Johansson, R. S., & Flanagan, J. R. (2009). *Coding and use of tactile signals from the hand in object manipulation*. Nature Reviews Neuroscience, 10(5), 345–359.
   - Verrillo, R. T. (1963). *Vibrotactile sensitivity and the frequency response of the Pacinian corpuscle*. Psychonomic Science, 4(1), 135–136.
   - Human mechanoreceptor band-pass characteristics:
     - **Pacinian corpuscles (FA II afferents)**: Mediate high-frequency vibrotactile perception across ~100 Hz – 400 Hz, exhibiting optimal psychophysical detection thresholds at **150 Hz – 250 Hz** (maximum displacement sensitivity at $\approx 200\text{--}250\text{ Hz}$).
     - **Meissner corpuscles (FA I afferents)**: Respond primarily to low-frequency skin indentation and flutter (10 Hz – 60 Hz).

---

## 3. Scientific Distinctions: Perception, Hardware, and Experimental Stimuli

To maintain scientific rigor, this architecture strictly distinguishes among three independent domains:
1. **Human Vibrotactile Perception Literature**
2. **Android Actuator & Device Capabilities**
3. **HaptiX Experimental Stimulus Frequencies**

### A. Human Vibrotactile Perception Literature (Psychophysics)
- **Mechanoreceptor Sensitivity**: Human tactile perception is governed by specialized mechanoreceptive afferents. High-frequency vibrotactile perception is mediated by **Pacinian corpuscles (FA II afferents)**, responding across ~100 Hz to 400 Hz.
- **Optimal Psychophysical Range**: Extensive psychophysical literature (Verrillo 1963; Choi & Kuchenbecker 2013) identifies the **150 Hz to 250 Hz** band as the region of lowest human detection thresholds (peak sensitivity around **200 Hz to 250 Hz**).
- **Perceptual Sensation**: Frequencies in the 200–250 Hz range produce crisp, salient tactile feedback with minimal skin displacement, whereas lower frequencies (<100 Hz) engage Meissner corpuscles, producing flutter or rumble sensations.

### B. Android Actuator & Device Hardware Capabilities
- **Narrowband Actuator Dynamics**: Mobile Linear Resonant Actuators (LRAs) function as mass-spring electromechanical resonators. Peak acceleration is produced at their physical resonant frequency ($f_0$). Driving an LRA off-resonance produces a steep decline in vibrational amplitude unless compensated by closed-loop driver ICs.
- **Device-Dependent Resonant Frequency**: **There is no universal resonant frequency across Android devices.** An actuator's resonant frequency $f_0$ is strictly hardware- and model-dependent, varying across manufacturers, motor geometry, spring stiffness, and device mass.
- **Runtime Hardware Querying**: Android does not specify or assume a fixed resonant frequency. Instead, the platform exposes dynamic hardware-query APIs:
  - `Vibrator.getResonantFrequency()`: Returns the device-specific resonant frequency in Hz (or `Float.NaN` if unsupported/composite).
  - `Vibrator.getFrequencyProfile()`: Returns the device-specific frequency-to-acceleration profile (`VibratorFrequencyProfile`).
  - Where supported (Android 16+ / API 36+), applications should dynamically obtain the actuator's true capabilities from these APIs rather than assuming fixed constants.

### C. HaptiX Experimental Stimulus Frequencies
- The frequencies in the HaptiX stimulus dataset (**150 Hz, 180 Hz, 200 Hz, 220 Hz, 235 Hz, and 250 Hz**) are **experimentally selected stimuli for this research application**, chosen to span the human psychophysical sensitivity curve and evaluate multimodal perception under controlled conditions.
- They are explicitly **not** universal Android actuator resonant frequencies.
- **Explicit Stimulus Provenance**:
  - `150 Hz`: *Perception-literature motivated & experimentally selected by HaptiX* — Represents the lower bound of the Pacinian sensitivity band; selected by HaptiX as a lower-frequency test condition.
  - `180 Hz`: *Experimentally selected by HaptiX* — Selected as an intermediate lower-mid stimulus frequency for study condition variation; not a universal actuator boundary.
  - `200 Hz`: *Perception-literature motivated* — Benchmark in psychophysics literature for the onset of peak Pacinian sensitivity (Choi & Kuchenbecker, 2013).
  - `220 Hz`: *Experimentally selected by HaptiX* — Selected as an intermediate upper-mid stimulus step.
  - `235 Hz`: *Experimentally selected by HaptiX* — Upper-band stimulus condition selected by HaptiX for experimental evaluation; actuator resonance is device-dependent and must be queried from Android hardware APIs.
  - `250 Hz`: *Perception-literature motivated* — Benchmark in human psychophysics literature for optimal displacement detection threshold (Verrillo, 1963).

---

## 4. Android API Levels & Platform Limitations

### SOURCE-DERIVED FACTS
- **Android 8.0 to Android 15 (API 26 – 35)**:
  - Supports `Vibrator.hasVibrator()` and `Vibrator.hasAmplitudeControl()`.
  - Supports `VibrationEffect.createOneShot(duration, amplitude)` and `createWaveform(timings, amplitudes, repeat)`.
  - Supports `VibrationEffect.Composition` (API 30+) for predefined system primitives (CLICK, TICK, THUD).
  - **Crucial Limitation**: Android does **NOT** expose arbitrary frequency modulation or frequency envelope builders in public APIs prior to Android 16. Actuators vibrate at their fixed hardware resonance.
- **Android 16+ (API 36+)**:
  - Introduces `VibrationEffect.WaveformEnvelopeBuilder` allowing simultaneous frequency (Hz) and amplitude (normalized Gs) parameterization.
  - Introduces `Vibrator.areEnvelopeEffectsSupported()`. If `false`, the platform will not play envelope effects and provides no automatic fallback.

### HAPTIX IMPLEMENTATION DECISIONS
- HaptiX uses safe runtime capability inspection (`AndroidHapticCapabilities`).
- On API 36+ devices with `areEnvelopeEffectsSupported() == true`, `AndroidHapticPlayer` compiles and dispatches `WaveformEnvelopeBuilder` control points.
- On API 26–35 or devices lacking envelope support, HaptiX **refuses to fake frequency control**. It flags the hardware status as `HAPTICS LIMITED` and actuates calibrated amplitude modulation at the actuator's resonant frequency, logging that frequency modulation was bypassed.

---

## 5. Hardware Capability Detection Architecture

HaptiX categorizes devices into three explicit tiers:

| Capability Tier | Hardware Condition | HaptiX Status Label | Actuation Behavior |
| :--- | :--- | :--- | :--- |
| **`SUPPORTED`** | `hasVibrator == true` AND `areEnvelopeEffectsSupported == true` | `● HAPTICS READY` | Frequency-aware envelope synthesis via `WaveformEnvelopeBuilder` |
| **`LIMITED`** | `hasVibrator == true` AND `areEnvelopeEffectsSupported == false` | `● HAPTICS LIMITED` | Amplitude-calibrated fallback at actuator resonance $f_0$; no fake frequency |
| **`UNAVAILABLE`**| `hasVibrator == false` or hardware absent | `● HAPTICS UNAVAILABLE`| All actuation disabled; safe bypass |

---

## 6. HaptiX Internal Frequency Data Schema

### SOURCE-DERIVED FACTS
- Android does **not** specify a canonical JSON schema for haptic frequency timelines.
- iOS uses Apple Haptic Audio Pattern (`.ahap`), which is proprietary to Apple CoreHaptics.

### HAPTIX IMPLEMENTATION DECISIONS
- HaptiX defines an explicit internal schema: `HaptiX-Frequency-Stimulus` v1.0.
- JSON structure encapsulates dataset provenance, citations, and validated points:
```json
{
  "_comment": "HaptiX internal research representation. NOT an official Android OS JSON format.",
  "format": "HaptiX-Frequency-Stimulus",
  "version": "1.0",
  "source": "AOSP Haptics Architecture Guidelines & Pacinian Corpuscle Psychophysics (Choi & Kuchenbecker, 2013)",
  "sourceUrl": "https://source.android.com/devices/sensors/haptics",
  "description": "Research-calibrated vibrotactile stimulus sequence for multimodal audiovisual evaluation...",
  "frequencyUnit": "Hz",
  "timingUnit": "ms",
  "videoId": "video_01",
  "points": [
    {
      "startTimeMs": 1500,
      "durationMs": 250,
      "frequencyHz": 150.0,
      "amplitude": 0.65,
      "description": "Tactile test pulse (150 Hz)",
      "provenance": "perception-literature motivated & experimentally selected by HaptiX"
    }
  ]
}
```
Validation rules enforced by `FrequencyPatternParser`:
- `frequencyHz > 0`, finite, non-NaN.
- `durationMs >= 0`, `startTimeMs >= 0`.
- `amplitude in 0.0f..1.0f`, finite.
- Chronological ordering enforced on load.

---

## 7. Audiovisual & Haptic Synchronization Architecture

```
Media3 ExoPlayer (currentPositionMs)
            ↓ (30ms polling & position callbacks)
VideoPlayerViewModel.onTimelinePositionChanged(posMs)
            ↓
HapticSynchronizer.onTimelineUpdate(posMs)
            ↓ (evaluates [startTimeMs, startTimeMs + toleranceMs])
AndroidHapticPlayer.playEvent(event)
            ↓ (checks HapticCapabilityLevel)
Hardware Actuator (Vibrator)
```

### Rapid Seeking & Scrubbing State Machine
1. **Forward Seeking**: `onSeek(seekPositionMs)` immediately calls `engine.stop()`, resets dispatch markers, and marks all events with `startTimeMs < seekPositionMs` as dispatched. Events prior to the seek timestamp will **never** fire inadvertently.
2. **Backward Seeking**: Marks events before `seekPositionMs` as dispatched, but restores all events $\ge \text{seekPositionMs}$ to eligible status.
3. **Deduplication**: During continuous forward playback, each event fires strictly once.
4. **Pause / End of Stream**: Active vibrations are immediately canceled via `vibrator.cancel()`.

---

## 8. Research Debug HUD

For scientific verification, HaptiX provides a discrete **Research Debug HUD** accessible to experimenters:
- **Current video position**: millisecond-accurate elapsed time (`mm:ss.SSS`).
- **Current haptic event**: Identifier and research description.
- **Frequency**: Measured target frequency (`XXX.X Hz`).
- **Amplitude**: Normalized acceleration ($0.00\text{--}1.00$).
- **Event duration**: Timeline pulse duration in milliseconds.
- **Haptic capability**: `SUPPORTED`, `LIMITED`, or `UNAVAILABLE`.

---

## 9. Known Limitations & Reproducibility Guidelines

1. **Physical Actuator Variance**: Even on devices with identical Android API levels, physical LRA motor mass, mounting rigidity, and phone chassis inertia vary significantly. Quantitative acceleration should ideally be verified with a laser Doppler vibrometer or calibrated accelerometer for high-precision physical studies.
2. **Physical Device Testing**: Because development was conducted in an automated software development environment without physical Android hardware present, physical tactile output was **not** tested on a physical phone. All software layers, capability fallbacks, and synchronizations were validated using automated unit test suites, Android capability harnesses, and build/lint validations.
3. **No AHAP Ingestion**: Per architectural requirements, AHAP files are reserved strictly for the iOS target. The Android software layer relies exclusively on this frequency data format.
