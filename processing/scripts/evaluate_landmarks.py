import json
import numpy as np
import pandas as pd

def evaluate_landmarks():
    # Load onsets
    with open("processing/haptic_sources/f1_2025/f1_onsets.csv", "r") as f:
        onset_sec = np.array([float(x.strip()) for x in f if x.strip()])
    onset_ms = np.round(onset_sec * 1000.0).astype(np.int64)
    
    # Load RMS
    df_rms = pd.read_csv("processing/haptic_sources/f1_2025/f1_rms.csv", header=None, names=['time_sec', 'rms'])
    rms_time_ms = np.round(df_rms['time_sec'].values * 1000.0).astype(np.int64)
    rms_vals = df_rms['rms'].values
    
    checkpoints = [
        {"name": "Acceleration Kick 1", "target_ms": 26640},
        {"name": "Acceleration Surge 1", "target_ms": 27140},
        {"name": "Engine Spool", "target_ms": 29900},
        {"name": "Overtake Surge", "target_ms": 30760},
        {"name": "Rapid Upshift Sub-Peak", "target_ms": 32520},
        {"name": "Rapid Upshift Main Peak", "target_ms": 32640},
        {"name": "Apex Curb Strike", "target_ms": 42860},
        {"name": "Exit Curb Strike", "target_ms": 44360},
        {"name": "High-G Turn Peak 1", "target_ms": 55620},
        {"name": "Race Shift / Curb", "target_ms": 73880},
        {"name": "Engine Rev Flyby", "target_ms": 80240},
        {"name": "Major Crash Impact", "target_ms": 105060},
        {"name": "Cinematic Bass Drop", "target_ms": 117520},
        {"name": "Climax Beat 1", "target_ms": 123520},
        {"name": "Climax Beat 2", "target_ms": 124020},
        {"name": "Climax Beat 3", "target_ms": 124520},
        {"name": "Climax Beat 4", "target_ms": 125020},
        {"name": "Climax Beat 5", "target_ms": 125520},
        {"name": "Climax Beat 6 (Final Sting)", "target_ms": 126020},
    ]
    
    results = []
    for cp in checkpoints:
        tgt = cp["target_ms"]
        # Nearest onset
        onset_diffs = onset_ms - tgt
        nearest_onset_idx = np.argmin(np.abs(onset_diffs))
        nearest_onset_ms = int(onset_ms[nearest_onset_idx])
        onset_offset_ms = int(onset_diffs[nearest_onset_idx])
        
        # Local RMS window: +/- 300ms
        win_mask = (rms_time_ms >= tgt - 300) & (rms_time_ms <= tgt + 300)
        win_times = rms_time_ms[win_mask]
        win_rms = rms_vals[win_mask]
        
        # RMS at target (nearest sample)
        tgt_sample_idx = np.argmin(np.abs(rms_time_ms - tgt))
        rms_at_tgt = float(rms_vals[tgt_sample_idx])
        time_at_tgt_sample = int(rms_time_ms[tgt_sample_idx])
        
        # Local peak in window
        local_max_idx = np.argmax(win_rms)
        local_max_time_ms = int(win_times[local_max_idx])
        local_max_rms = float(win_rms[local_max_idx])
        rms_peak_offset_ms = int(local_max_time_ms - tgt)
        
        # Local baseline (10th percentile in window)
        local_baseline = float(np.percentile(win_rms, 10))
        local_prominence = float(local_max_rms - local_baseline)
        
        # Characterize behavior
        if local_max_rms > 0.05:
            energy_level = "VERY_HIGH"
        elif local_max_rms > 0.035:
            energy_level = "HIGH"
        elif local_max_rms > 0.02:
            energy_level = "MODERATE"
        else:
            energy_level = "LOW"
            
        results.append({
            "name": cp["name"],
            "target_ms": tgt,
            "nearest_onset_ms": nearest_onset_ms,
            "onset_offset_ms": onset_offset_ms,
            "rms_at_target": rms_at_tgt,
            "time_at_target_sample": time_at_tgt_sample,
            "local_max_time_ms": local_max_time_ms,
            "local_max_rms": local_max_rms,
            "rms_peak_offset_ms": rms_peak_offset_ms,
            "local_baseline": local_baseline,
            "local_prominence": local_prominence,
            "energy_level": energy_level
        })
        
    with open("processing/output/f1_2025/f1_landmarks_evaluation.json", "w") as f:
        json.dump(results, f, indent=2)
        
    print(f"Evaluated {len(results)} landmarks:")
    for r in results:
        print(f"  [{r['target_ms']} ms] {r['name']:<25} | Near Onset: {r['nearest_onset_ms']}ms (delta {r['onset_offset_ms']:+3d}ms) | Local RMS Peak: {r['local_max_time_ms']}ms ({r['local_max_rms']:.4f}, delta {r['rms_peak_offset_ms']:+3d}ms) | {r['energy_level']}")

if __name__ == "__main__":
    evaluate_landmarks()
