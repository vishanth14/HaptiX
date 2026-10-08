import os
import json
import numpy as np
import pandas as pd

def build_detailed_onsets_csv():
    # Load onsets
    with open("processing/haptic_sources/f1_2025/f1_onsets.csv", "r") as f:
        onset_sec = np.array([float(x.strip()) for x in f if x.strip()])
    onset_ms = np.round(onset_sec * 1000.0).astype(np.int64)
    
    # Load RMS
    df_rms = pd.read_csv("processing/haptic_sources/f1_2025/f1_rms.csv", header=None, names=['time_sec', 'rms'])
    rms_time_ms = np.round(df_rms['time_sec'].values * 1000.0).astype(np.int64)
    rms_vals = df_rms['rms'].values
    hop_ms = float(np.median(np.diff(rms_time_ms)))
    
    detailed = []
    for i, t_ms in enumerate(onset_ms):
        # Sample idx
        idx = np.argmin(np.abs(rms_time_ms - t_ms))
        rms_at_onset = float(rms_vals[idx])
        
        # Local window [-100ms, +100ms]
        win_mask = (rms_time_ms >= t_ms - 100) & (rms_time_ms <= t_ms + 100)
        win_rms = rms_vals[win_mask] if np.any(win_mask) else np.array([rms_at_onset])
        
        local_max_rms = float(np.max(win_rms))
        local_min_rms = float(np.min(win_rms))
        local_rise = float(local_max_rms - local_min_rms)
        
        # Inter-onset interval
        pre_ioi = int(t_ms - onset_ms[i-1]) if i > 0 else -1
        post_ioi = int(onset_ms[i+1] - t_ms) if i < len(onset_ms) - 1 else -1
        
        detailed.append({
            "index": i + 1,
            "timestamp_sec": float(onset_sec[i]),
            "timestamp_ms": int(t_ms),
            "rms_at_onset": round(rms_at_onset, 6),
            "local_max_rms": round(local_max_rms, 6),
            "local_rise": round(local_rise, 6),
            "pre_ioi_ms": pre_ioi,
            "post_ioi_ms": post_ioi
        })
        
    df_out = pd.DataFrame(detailed)
    df_out.to_csv("processing/output/f1_2025/f1_onsets_detailed.csv", index=False)
    print(f"Saved processing/output/f1_2025/f1_onsets_detailed.csv ({len(df_out)} rows)")

def build_final_json():
    # Load landmark evaluations
    with open("processing/output/f1_2025/f1_landmarks_evaluation.json", "r") as f:
        landmarks = json.load(f)
    landmark_map = {l["target_ms"]: l for l in landmarks}
    
    # 29 Master Authored Events
    events_spec = [
        {
            "id": "f1_final_001",
            "type": "CONTINUOUS",
            "startTimeMs": 1040,
            "peakTimeMs": 1060,
            "durationMs": 860,
            "intensity": 0.178,
            "sharpness": 0.35,
            "semanticType": "CINEMATIC_ACCENT",
            "attackMs": 140,
            "sustainMs": 420,
            "releaseMs": 300,
            "confidence": 0.92,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "Initial orchestral and sub-bass tension swell marking trailer opening",
            "landmark": None
        },
        {
            "id": "f1_final_002",
            "type": "CONTINUOUS",
            "startTimeMs": 6040,
            "peakTimeMs": 6680,
            "durationMs": 1040,
            "intensity": 0.315,
            "sharpness": 0.28,
            "semanticType": "LOW_FREQUENCY_DRONE",
            "attackMs": 240,
            "sustainMs": 560,
            "releaseMs": 240,
            "confidence": 0.90,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "Low-frequency pit garage idle rumble and mechanic equipment presence",
            "landmark": None
        },
        {
            "id": "f1_final_003",
            "type": "CONTINUOUS",
            "startTimeMs": 7660,
            "peakTimeMs": 8020,
            "durationMs": 1940,
            "intensity": 0.396,
            "sharpness": 0.45,
            "semanticType": "ENGINE_RUMBLE",
            "attackMs": 360,
            "sustainMs": 980,
            "releaseMs": 600,
            "confidence": 0.94,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "APXGP hybrid V6 starter motor cough, initial combustion rev, and throttle blip",
            "landmark": None
        },
        {
            "id": "f1_final_004",
            "type": "TRANSIENT",
            "startTimeMs": 10960,
            "peakTimeMs": 11100,
            "durationMs": 280,
            "intensity": 0.212,
            "sharpness": 0.70,
            "semanticType": "GEAR_SHIFT",
            "attackMs": 30,
            "sustainMs": 110,
            "releaseMs": 140,
            "confidence": 0.91,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "First gear engagement mechanical transient before pit exit blend line",
            "landmark": None
        },
        {
            "id": "f1_final_005",
            "type": "CONTINUOUS",
            "startTimeMs": 12200,
            "peakTimeMs": 13060,
            "durationMs": 3000,
            "intensity": 0.286,
            "sharpness": 0.40,
            "semanticType": "ACCELERATION_RISE",
            "attackMs": 860,
            "sustainMs": 1280,
            "releaseMs": 860,
            "confidence": 0.95,
            "curveIn": "EASE_IN",
            "curveOut": "EASE_OUT",
            "description": "Pit lane exit acceleration surge climbing to circuit blending speed",
            "landmark": None
        },
        {
            "id": "f1_final_006",
            "type": "CONTINUOUS",
            "startTimeMs": 18200,
            "peakTimeMs": 20860,
            "durationMs": 3200,
            "intensity": 0.330,
            "sharpness": 0.48,
            "semanticType": "ENGINE_RUMBLE",
            "attackMs": 960,
            "sustainMs": 1440,
            "releaseMs": 800,
            "confidence": 0.93,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "Mid-track turbocharger spool and acoustic power surge approaching braking sector",
            "landmark": None
        },
        {
            "id": "f1_final_007",
            "type": "TRANSIENT",
            "startTimeMs": 26540,
            "peakTimeMs": 26640,
            "durationMs": 260,
            "intensity": 0.447,
            "sharpness": 0.80,
            "semanticType": "GEAR_SHIFT",
            "attackMs": 20,
            "sustainMs": 80,
            "releaseMs": 160,
            "confidence": 0.96,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Aggressive paddle upshift transient into high-speed acceleration sector",
            "landmark": 26640
        },
        {
            "id": "f1_final_008",
            "type": "CONTINUOUS",
            "startTimeMs": 27040,
            "peakTimeMs": 27140,
            "durationMs": 560,
            "intensity": 0.469,
            "sharpness": 0.52,
            "semanticType": "ACCELERATION_RISE",
            "attackMs": 100,
            "sustainMs": 260,
            "releaseMs": 200,
            "confidence": 0.95,
            "curveIn": "EASE_IN",
            "curveOut": "EASE_OUT",
            "description": "Engine torque surge following upshift under heavy circuit acceleration",
            "landmark": 27140
        },
        {
            "id": "f1_final_009",
            "type": "CONTINUOUS",
            "startTimeMs": 29600,
            "peakTimeMs": 29900,
            "durationMs": 700,
            "intensity": 0.442,
            "sharpness": 0.55,
            "semanticType": "ENGINE_RUMBLE",
            "attackMs": 200,
            "sustainMs": 300,
            "releaseMs": 200,
            "confidence": 0.95,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "High RPM engine spool building velocity toward slipstream overtake",
            "landmark": 29900
        },
        {
            "id": "f1_final_010",
            "type": "CONTINUOUS",
            "startTimeMs": 30640,
            "peakTimeMs": 30760,
            "durationMs": 660,
            "intensity": 0.500,
            "sharpness": 0.58,
            "semanticType": "ACCELERATION_RISE",
            "attackMs": 120,
            "sustainMs": 320,
            "releaseMs": 220,
            "confidence": 0.96,
            "curveIn": "EASE_IN",
            "curveOut": "EASE_OUT",
            "description": "Slipstream overtake aerodynamic shockwave and full power burst",
            "landmark": 30760
        },
        {
            "id": "f1_final_011",
            "type": "TRANSIENT",
            "startTimeMs": 32440,
            "peakTimeMs": 32640,
            "durationMs": 280,
            "intensity": 0.479,
            "sharpness": 0.82,
            "semanticType": "GEAR_SHIFT",
            "attackMs": 20,
            "sustainMs": 100,
            "releaseMs": 160,
            "confidence": 0.96,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Rapid double upshift transient kick under maximum straightline acceleration load",
            "landmark": 32640
        },
        {
            "id": "f1_final_012",
            "type": "CONTINUOUS",
            "startTimeMs": 39000,
            "peakTimeMs": 39060,
            "durationMs": 400,
            "intensity": 0.222,
            "sharpness": 0.45,
            "semanticType": "TRANSITION",
            "attackMs": 60,
            "sustainMs": 180,
            "releaseMs": 160,
            "confidence": 0.88,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "Pit wall comms radio click and cinematic scene transition",
            "landmark": None
        },
        {
            "id": "f1_final_013",
            "type": "TRANSIENT",
            "startTimeMs": 41240,
            "peakTimeMs": 41400,
            "durationMs": 320,
            "intensity": 0.270,
            "sharpness": 0.65,
            "semanticType": "LIGHT_IMPACT",
            "attackMs": 25,
            "sustainMs": 95,
            "releaseMs": 200,
            "confidence": 0.90,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Downshift throttle blip and chassis mechanical settling into turn approach",
            "landmark": None
        },
        {
            "id": "f1_final_014",
            "type": "TRANSIENT",
            "startTimeMs": 42760,
            "peakTimeMs": 42860,
            "durationMs": 260,
            "intensity": 0.438,
            "sharpness": 0.78,
            "semanticType": "CURB_RUMBLE",
            "attackMs": 25,
            "sustainMs": 95,
            "releaseMs": 140,
            "confidence": 0.95,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Apex curb strike sending sharp high-frequency tactile vibrations through suspension",
            "landmark": 42860
        },
        {
            "id": "f1_final_015",
            "type": "TRANSIENT",
            "startTimeMs": 44260,
            "peakTimeMs": 44360,
            "durationMs": 260,
            "intensity": 0.430,
            "sharpness": 0.76,
            "semanticType": "CURB_RUMBLE",
            "attackMs": 25,
            "sustainMs": 95,
            "releaseMs": 140,
            "confidence": 0.95,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Exit curb strike and wheel hop transient as car straightens for traction",
            "landmark": 44360
        },
        {
            "id": "f1_final_016",
            "type": "CONTINUOUS",
            "startTimeMs": 55200,
            "peakTimeMs": 55620,
            "durationMs": 1200,
            "intensity": 0.528,
            "sharpness": 0.60,
            "semanticType": "CONTINUOUS_RUMBLE",
            "attackMs": 360,
            "sustainMs": 540,
            "releaseMs": 300,
            "confidence": 0.96,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "Sustained lateral G-force grip vibration and high-rev engine scream through sweeping turn",
            "landmark": 55620
        },
        {
            "id": "f1_final_017",
            "type": "CONTINUOUS",
            "startTimeMs": 61640,
            "peakTimeMs": 61880,
            "durationMs": 520,
            "intensity": 0.370,
            "sharpness": 0.50,
            "semanticType": "FAST_MOTION",
            "attackMs": 140,
            "sustainMs": 240,
            "releaseMs": 140,
            "confidence": 0.92,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "Trackside camera high-speed aerodynamic flyby whoosh",
            "landmark": None
        },
        {
            "id": "f1_final_018",
            "type": "TRANSIENT",
            "startTimeMs": 73760,
            "peakTimeMs": 73880,
            "durationMs": 280,
            "intensity": 0.460,
            "sharpness": 0.80,
            "semanticType": "GEAR_SHIFT",
            "attackMs": 20,
            "sustainMs": 100,
            "releaseMs": 160,
            "confidence": 0.95,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Sharp curb hop and simultaneous gear shift in wheel-to-wheel race combat",
            "landmark": 73880
        },
        {
            "id": "f1_final_019",
            "type": "CONTINUOUS",
            "startTimeMs": 80120,
            "peakTimeMs": 80240,
            "durationMs": 280,
            "intensity": 0.453,
            "sharpness": 0.55,
            "semanticType": "ENGINE_RUMBLE",
            "attackMs": 60,
            "sustainMs": 120,
            "releaseMs": 100,
            "confidence": 0.94,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "SMOOTHSTEP",
            "description": "On-board camera close pass with engine Doppler frequency surge",
            "landmark": 80240
        },
        {
            "id": "f1_final_020",
            "type": "CONTINUOUS",
            "startTimeMs": 102100,
            "peakTimeMs": 102440,
            "durationMs": 500,
            "intensity": 0.364,
            "sharpness": 0.65,
            "semanticType": "TENSION_BUILD",
            "attackMs": 300,
            "sustainMs": 120,
            "releaseMs": 80,
            "confidence": 0.94,
            "curveIn": "SMOOTHSTEP",
            "curveOut": "EASE_OUT",
            "description": "Pre-crash rising audio scream and tire lockup screech right before impact",
            "landmark": None
        },
        {
            "id": "f1_final_021",
            "type": "TRANSIENT",
            "startTimeMs": 105020,
            "peakTimeMs": 105060,
            "durationMs": 180,
            "intensity": 0.744,
            "sharpness": 0.90,
            "semanticType": "COLLISION",
            "attackMs": 20,
            "sustainMs": 70,
            "releaseMs": 90,
            "confidence": 0.98,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "MAJOR CRASH IMPACT: violent high-G primary vehicle collision with barrier",
            "landmark": 105060
        },
        {
            "id": "f1_final_022",
            "type": "CONTINUOUS",
            "startTimeMs": 105240,
            "peakTimeMs": 105300,
            "durationMs": 1560,
            "intensity": 0.271,
            "sharpness": 0.38,
            "semanticType": "CRASH_AFTERSHOCK",
            "attackMs": 60,
            "sustainMs": 500,
            "releaseMs": 1000,
            "confidence": 0.93,
            "curveIn": "EASE_OUT",
            "curveOut": "EASE_OUT",
            "description": "Crash aftermath: tumbling chassis, gravel trap deceleration, and secondary debris dissipation",
            "landmark": None
        },
        {
            "id": "f1_final_023",
            "type": "TRANSIENT",
            "startTimeMs": 117420,
            "peakTimeMs": 117520,
            "durationMs": 260,
            "intensity": 0.452,
            "sharpness": 0.72,
            "semanticType": "CINEMATIC_ACCENT",
            "attackMs": 20,
            "sustainMs": 80,
            "releaseMs": 160,
            "confidence": 0.95,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Cinematic heavy sub-bass drop and dramatic audio cut to black",
            "landmark": 117520
        },
        {
            "id": "f1_final_024",
            "type": "TRANSIENT",
            "startTimeMs": 123440,
            "peakTimeMs": 123520,
            "durationMs": 240,
            "intensity": 0.435,
            "sharpness": 0.75,
            "semanticType": "HEAVY_IMPACT",
            "attackMs": 20,
            "sustainMs": 80,
            "releaseMs": 140,
            "confidence": 0.96,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Climax rhythm sequence Beat 1: trailer percussion impact",
            "landmark": 123520
        },
        {
            "id": "f1_final_025",
            "type": "TRANSIENT",
            "startTimeMs": 123940,
            "peakTimeMs": 124020,
            "durationMs": 220,
            "intensity": 0.529,
            "sharpness": 0.78,
            "semanticType": "HEAVY_IMPACT",
            "attackMs": 20,
            "sustainMs": 70,
            "releaseMs": 130,
            "confidence": 0.96,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Climax rhythm sequence Beat 2: trailer percussion impact (+500ms)",
            "landmark": 124020
        },
        {
            "id": "f1_final_026",
            "type": "TRANSIENT",
            "startTimeMs": 124440,
            "peakTimeMs": 124520,
            "durationMs": 220,
            "intensity": 0.567,
            "sharpness": 0.80,
            "semanticType": "HEAVY_IMPACT",
            "attackMs": 20,
            "sustainMs": 70,
            "releaseMs": 130,
            "confidence": 0.97,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Climax rhythm sequence Beat 3: trailer percussion impact (+1000ms)",
            "landmark": 124520
        },
        {
            "id": "f1_final_027",
            "type": "TRANSIENT",
            "startTimeMs": 124940,
            "peakTimeMs": 125020,
            "durationMs": 220,
            "intensity": 0.592,
            "sharpness": 0.82,
            "semanticType": "HEAVY_IMPACT",
            "attackMs": 20,
            "sustainMs": 70,
            "releaseMs": 130,
            "confidence": 0.97,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Climax rhythm sequence Beat 4: trailer percussion impact (+1500ms)",
            "landmark": 125020
        },
        {
            "id": "f1_final_028",
            "type": "TRANSIENT",
            "startTimeMs": 125440,
            "peakTimeMs": 125520,
            "durationMs": 240,
            "intensity": 0.641,
            "sharpness": 0.85,
            "semanticType": "HEAVY_IMPACT",
            "attackMs": 20,
            "sustainMs": 80,
            "releaseMs": 140,
            "confidence": 0.98,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Climax rhythm sequence Beat 5: trailer percussion impact (+2000ms)",
            "landmark": 125520
        },
        {
            "id": "f1_final_029",
            "type": "TRANSIENT",
            "startTimeMs": 125920,
            "peakTimeMs": 126020,
            "durationMs": 300,
            "intensity": 0.646,
            "sharpness": 0.88,
            "semanticType": "HEAVY_IMPACT",
            "attackMs": 25,
            "sustainMs": 100,
            "releaseMs": 175,
            "confidence": 0.98,
            "curveIn": "LINEAR",
            "curveOut": "EASE_OUT",
            "description": "Climax final title card percussion hit and main sting (+2500ms)",
            "landmark": 126020
        }
    ]
    
    # Load onsets to find nearest measured onset for each event
    with open("processing/haptic_sources/f1_2025/f1_onsets.csv", "r") as f:
        onset_sec = np.array([float(x.strip()) for x in f if x.strip()])
    onset_ms = np.round(onset_sec * 1000.0).astype(np.int64)
    
    # Load RMS to find local peak value
    df_rms = pd.read_csv("processing/haptic_sources/f1_2025/f1_rms.csv", header=None, names=['time_sec', 'rms'])
    rms_time_ms = np.round(df_rms['time_sec'].values * 1000.0).astype(np.int64)
    rms_vals = df_rms['rms'].values
    
    final_events = []
    for spec in events_spec:
        s_ms = spec["startTimeMs"]
        p_ms = spec["peakTimeMs"]
        d_ms = spec["durationMs"]
        att = spec["attackMs"]
        sus = spec["sustainMs"]
        rel = spec["releaseMs"]
        
        # Verify envelope sum == durationMs exactly
        assert att + sus + rel == d_ms, f"Envelope sum mismatch in {spec['id']}: {att}+{sus}+{rel} != {d_ms}"
        
        # Nearest measured onset
        diffs = onset_ms - p_ms
        near_idx = np.argmin(np.abs(diffs))
        near_onset = int(onset_ms[near_idx])
        near_offset = int(diffs[near_idx])
        
        # Nearest RMS sample at peak
        rms_idx = np.argmin(np.abs(rms_time_ms - p_ms))
        rms_at_peak = float(rms_vals[rms_idx])
        
        # Construct parameters
        params = {
            "description": spec["description"],
            "provenance": "Derived from f1_onsets.csv + f1_rms.csv; perceptual envelope authoring",
            "nearestOnsetMs": near_onset,
            "nearestOnsetOffsetMs": near_offset,
            "localRmsAtPeak": round(rms_at_peak, 6),
            "envelope": {
                "attackMs": att,
                "sustainMs": sus,
                "releaseMs": rel,
                "curveIn": spec["curveIn"],
                "curveOut": spec["curveOut"]
            }
        }
        if spec["landmark"] is not None:
            params["verifiedLandmarkMs"] = spec["landmark"]
            
        event_dict = {
            "id": spec["id"],
            "type": spec["type"],
            "startTimeMs": s_ms,
            "peakTimeMs": p_ms,
            "durationMs": d_ms,
            "intensity": spec["intensity"],
            "sharpness": spec["sharpness"],
            "semanticType": spec["semanticType"],
            "attackMs": att,
            "sustainMs": sus,
            "releaseMs": rel,
            "confidence": spec["confidence"],
            "sourceModalities": [
                "audio_onset",
                "audio_rms"
            ],
            "parameters": params
        }
        final_events.append(event_dict)
        
    final_data = {
        "videoId": "f1_2025_haptic_trailer",
        "version": "FINAL",
        "source": "AUDIO_DERIVED",
        "videoDurationMs": 130540,
        "analysis": {
            "onsetFile": "f1_onsets.csv",
            "rmsFile": "f1_rms.csv",
            "method": "Onset + RMS perceptual event extraction",
            "frequencyMeasurementAvailable": False
        },
        "events": final_events
    }
    
    # Save to android assets
    android_target = "android/app/src/main/assets/haptics/f1_2025_haptic_representation_final.json"
    with open(android_target, "w", encoding="utf-8") as f:
        json.dump(final_data, f, indent=2)
    print(f"Saved {android_target} ({len(final_events)} events)")
    
    # Save copy to processing/output/f1_2025/
    processing_target = "processing/output/f1_2025/f1_2025_haptic_representation_final.json"
    with open(processing_target, "w", encoding="utf-8") as f:
        json.dump(final_data, f, indent=2)
    print(f"Saved {processing_target} ({len(final_events)} events)")
    
    return final_data

if __name__ == "__main__":
    build_detailed_onsets_csv()
    build_final_json()
