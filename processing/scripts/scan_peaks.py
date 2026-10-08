import json
import numpy as np
import pandas as pd

def scan_all_peaks():
    # Load RMS
    df = pd.read_csv("processing/haptic_sources/f1_2025/f1_rms.csv", header=None, names=['time_sec', 'rms'])
    time_ms = np.round(df['time_sec'].values * 1000.0).astype(np.int64)
    rms = df['rms'].values
    
    # Load 29 events
    with open("android/app/src/main/assets/haptics/f1_2025_haptic_trailer_haptic_representation_v5.json", "r") as f:
        v29 = json.load(f)
    events_v29 = v29['events']
    
    # Find all regions with RMS > 0.04 (90th percentile is 0.048)
    high_rms_mask = rms > 0.045
    
    # For every event in v29, find the time span
    covered_mask = np.zeros(len(rms), dtype=bool)
    for e in events_v29:
        s = e['startTimeMs']
        end = s + e['durationMs']
        covered_mask |= ((time_ms >= s) & (time_ms <= end))
        
    uncovered_high_rms = high_rms_mask & (~covered_mask)
    uncovered_times = time_ms[uncovered_high_rms]
    uncovered_rms = rms[uncovered_high_rms]
    
    print(f"Total RMS samples: {len(rms)}")
    print(f"Samples > 0.045: {np.sum(high_rms_mask)} ({np.mean(high_rms_mask)*100:.1f}%)")
    print(f"Samples > 0.045 covered by 29 events: {np.sum(high_rms_mask & covered_mask)}")
    print(f"Uncovered high RMS samples: {len(uncovered_times)}")
    
    # Group uncovered samples into contiguous clusters
    if len(uncovered_times) > 0:
        clusters = []
        cur = [uncovered_times[0]]
        cur_rms = [uncovered_rms[0]]
        for i in range(1, len(uncovered_times)):
            if uncovered_times[i] - uncovered_times[i-1] <= 100:
                cur.append(uncovered_times[i])
                cur_rms.append(uncovered_rms[i])
            else:
                clusters.append((cur[0], cur[-1], len(cur), max(cur_rms)))
                cur = [uncovered_times[i]]
                cur_rms = [uncovered_rms[i]]
        clusters.append((cur[0], cur[-1], len(cur), max(cur_rms)))
        
        print("\nUncovered high-energy clusters (>0.045):")
        for c in clusters:
            print(f"  {c[0]}ms - {c[1]}ms ({c[1]-c[0]}ms, {c[2]} samples, max_rms={c[3]:.4f})")
    else:
        print("\nAll high-energy samples are covered!")

if __name__ == "__main__":
    scan_all_peaks()
