import os
import json
import numpy as np
import pandas as pd
from scipy.signal import find_peaks

def analyze_onsets(onset_path):
    # Load onsets
    with open(onset_path, 'r') as f:
        lines = [line.strip() for line in f if line.strip()]
    
    onset_sec = np.array([float(x) for x in lines])
    onset_ms = np.round(onset_sec * 1000.0).astype(np.int64)
    
    n_rows = len(onset_sec)
    first_sec, first_ms = float(onset_sec[0]), int(onset_ms[0])
    last_sec, last_ms = float(onset_sec[-1]), int(onset_ms[-1])
    
    # Check duplicates and sorting
    duplicates = int(np.sum(np.diff(onset_ms) == 0))
    is_sorted = bool(np.all(np.diff(onset_sec) >= 0))
    strictly_sorted = bool(np.all(np.diff(onset_sec) > 0))
    
    # Inter-onset intervals (ms)
    iois_ms = np.diff(onset_ms)
    
    ioi_stats = {
        "min_ms": int(np.min(iois_ms)),
        "max_ms": int(np.max(iois_ms)),
        "mean_ms": float(np.mean(iois_ms)),
        "median_ms": float(np.median(iois_ms)),
        "std_ms": float(np.std(iois_ms)),
        "p10_ms": float(np.percentile(iois_ms, 10)),
        "p25_ms": float(np.percentile(iois_ms, 25)),
        "p50_ms": float(np.percentile(iois_ms, 50)),
        "p75_ms": float(np.percentile(iois_ms, 75)),
        "p90_ms": float(np.percentile(iois_ms, 90)),
        "p95_ms": float(np.percentile(iois_ms, 95)),
        "p99_ms": float(np.percentile(iois_ms, 99)),
    }
    
    # Onset clusters (bursts where successive onsets are <= 150ms apart)
    clusters = []
    current_cluster = [int(onset_ms[0])]
    for i in range(1, n_rows):
        if onset_ms[i] - onset_ms[i-1] <= 150:
            current_cluster.append(int(onset_ms[i]))
        else:
            if len(current_cluster) > 1:
                clusters.append({
                    "start_ms": current_cluster[0],
                    "end_ms": current_cluster[-1],
                    "span_ms": current_cluster[-1] - current_cluster[0],
                    "count": len(current_cluster),
                    "onsets_ms": current_cluster
                })
            current_cluster = [int(onset_ms[i])]
    if len(current_cluster) > 1:
        clusters.append({
            "start_ms": current_cluster[0],
            "end_ms": current_cluster[-1],
            "span_ms": current_cluster[-1] - current_cluster[0],
            "count": len(current_cluster),
            "onsets_ms": current_cluster
        })
        
    # Isolated onsets (gap before >= 800ms and gap after >= 800ms)
    isolated = []
    for i in range(n_rows):
        pre_gap = float('inf') if i == 0 else onset_ms[i] - onset_ms[i-1]
        post_gap = float('inf') if i == n_rows - 1 else onset_ms[i+1] - onset_ms[i]
        if pre_gap >= 800 and post_gap >= 800:
            isolated.append({
                "index": i,
                "onset_ms": int(onset_ms[i]),
                "pre_gap_ms": int(pre_gap) if pre_gap != float('inf') else None,
                "post_gap_ms": int(post_gap) if post_gap != float('inf') else None
            })
            
    # Activity density across 1-second bins (0 to 131 seconds)
    bin_edges = np.arange(0, 132, 1.0)
    density, _ = np.histogram(onset_sec, bins=bin_edges)
    
    dense_regions = []
    sparse_regions = []
    for sec_idx, count in enumerate(density):
        if count >= 4:
            dense_regions.append({"second": sec_idx, "time_range_ms": [sec_idx * 1000, (sec_idx + 1) * 1000], "onset_count": int(count)})
        elif count == 0:
            sparse_regions.append({"second": sec_idx, "time_range_ms": [sec_idx * 1000, (sec_idx + 1) * 1000], "onset_count": 0})

    # Repeated rhythmic structures (e.g. 480-520ms intervals in climax)
    climax_candidates = []
    for i in range(len(iois_ms)):
        if 450 <= iois_ms[i] <= 550:
            climax_candidates.append({
                "from_onset_ms": int(onset_ms[i]),
                "to_onset_ms": int(onset_ms[i+1]),
                "ioi_ms": int(iois_ms[i])
            })
            
    result = {
        "n_rows": n_rows,
        "first_timestamp_sec": first_sec,
        "first_timestamp_ms": first_ms,
        "last_timestamp_sec": last_sec,
        "last_timestamp_ms": last_ms,
        "duplicates": duplicates,
        "is_sorted": is_sorted,
        "strictly_sorted": strictly_sorted,
        "ioi_stats": ioi_stats,
        "cluster_count": len(clusters),
        "clusters": clusters,
        "isolated_count": len(isolated),
        "isolated": isolated,
        "dense_regions_count": len(dense_regions),
        "dense_regions": dense_regions,
        "sparse_regions_count": len(sparse_regions),
        "sparse_regions": sparse_regions,
        "rhythmic_500ms_intervals": climax_candidates,
        "onsets_ms": [int(x) for x in onset_ms]
    }
    return result, onset_ms, onset_sec

def analyze_rms(rms_path):
    df = pd.read_csv(rms_path, header=None, names=['time_sec', 'rms'])
    time_sec = df['time_sec'].values
    time_ms = np.round(time_sec * 1000.0).astype(np.int64)
    rms = df['rms'].values
    n_rows = len(df)
    
    # Hop size
    time_diffs_sec = np.diff(time_sec)
    time_diffs_ms = np.diff(time_sec * 1000.0)
    hop_sec = float(np.median(time_diffs_sec))
    hop_ms = float(np.median(time_diffs_ms))
    
    # Range and basic stats
    rms_min = float(np.min(rms))
    rms_max = float(np.max(rms))
    rms_mean = float(np.mean(rms))
    rms_median = float(np.median(rms))
    rms_std = float(np.std(rms))
    
    percentiles = {
        "p01": float(np.percentile(rms, 1)),
        "p05": float(np.percentile(rms, 5)),
        "p10": float(np.percentile(rms, 10)),
        "p25": float(np.percentile(rms, 25)),
        "p50": float(np.percentile(rms, 50)),
        "p75": float(np.percentile(rms, 75)),
        "p90": float(np.percentile(rms, 90)),
        "p95": float(np.percentile(rms, 95)),
        "p99": float(np.percentile(rms, 99)),
        "p99_9": float(np.percentile(rms, 99.9)),
    }
    
    # Local maxima (peaks)
    # Using scipy find_peaks with prominence based on std
    peaks_idx, peak_props = find_peaks(rms, distance=int(100 / hop_ms), prominence=0.005)
    peaks_time_ms = time_ms[peaks_idx]
    peaks_rms = rms[peaks_idx]
    prominences = peak_props['prominences']
    
    # Local minima
    minima_idx, _ = find_peaks(-rms, distance=int(100 / hop_ms), prominence=0.003)
    
    # Rolling baseline (rolling 10th percentile over ~1.0 second = ~94 samples)
    window_samples = int(np.round(1000.0 / hop_ms))
    rolling_series = pd.Series(rms)
    rolling_baseline = rolling_series.rolling(window_samples, center=True, min_periods=1).quantile(0.10).values
    rolling_mean = rolling_series.rolling(window_samples, center=True, min_periods=1).mean().values
    
    # Slope (first difference / hop_sec)
    rms_slope = np.gradient(rms, hop_sec)
    
    # Near-silence regions (rms < 0.005)
    is_silent = rms < 0.005
    silent_regions = []
    in_silent = False
    start_idx = 0
    for i, s in enumerate(is_silent):
        if s and not in_silent:
            in_silent = True
            start_idx = i
        elif not s and in_silent:
            in_silent = False
            duration_ms = int(time_ms[i-1] - time_ms[start_idx])
            if duration_ms >= 300: # at least 300ms
                silent_regions.append({
                    "start_ms": int(time_ms[start_idx]),
                    "end_ms": int(time_ms[i-1]),
                    "duration_ms": duration_ms,
                    "mean_rms": float(np.mean(rms[start_idx:i]))
                })
    if in_silent:
        duration_ms = int(time_ms[-1] - time_ms[start_idx])
        if duration_ms >= 300:
            silent_regions.append({
                "start_ms": int(time_ms[start_idx]),
                "end_ms": int(time_ms[-1]),
                "duration_ms": duration_ms,
                "mean_rms": float(np.mean(rms[start_idx:]))
            })
            
    # Sustained energy regions (rms > p75 for >= 400ms)
    p75_val = percentiles['p75']
    is_sustained = rms >= p75_val
    sustained_regions = []
    in_sust = False
    start_s = 0
    for i, s in enumerate(is_sustained):
        if s and not in_sust:
            in_sust = True
            start_s = i
        elif not s and in_sust:
            in_sust = False
            dur_ms = int(time_ms[i-1] - time_ms[start_s])
            if dur_ms >= 300:
                sustained_regions.append({
                    "start_ms": int(time_ms[start_s]),
                    "end_ms": int(time_ms[i-1]),
                    "duration_ms": dur_ms,
                    "max_rms": float(np.max(rms[start_s:i])),
                    "mean_rms": float(np.mean(rms[start_s:i]))
                })
    if in_sust:
        dur_ms = int(time_ms[-1] - time_ms[start_s])
        if dur_ms >= 300:
            sustained_regions.append({
                "start_ms": int(time_ms[start_s]),
                "end_ms": int(time_ms[-1]),
                "duration_ms": dur_ms,
                "max_rms": float(np.max(rms[start_s:])),
                "mean_rms": float(np.mean(rms[start_s:]))
            })

    result = {
        "n_rows": n_rows,
        "start_sec": float(time_sec[0]),
        "start_ms": int(time_ms[0]),
        "end_sec": float(time_sec[-1]),
        "end_ms": int(time_ms[-1]),
        "hop_sec": hop_sec,
        "hop_ms": hop_ms,
        "rms_min": rms_min,
        "rms_max": rms_max,
        "rms_mean": rms_mean,
        "rms_median": rms_median,
        "rms_std": rms_std,
        "percentiles": percentiles,
        "local_maxima_count": len(peaks_idx),
        "local_minima_count": len(minima_idx),
        "silent_regions_count": len(silent_regions),
        "silent_regions": silent_regions,
        "sustained_regions_count": len(sustained_regions),
        "sustained_regions": sustained_regions,
        "peaks_top": [
            {
                "time_ms": int(peaks_time_ms[k]),
                "rms": float(peaks_rms[k]),
                "prominence": float(prominences[k])
            }
            for k in np.argsort(prominences)[::-1][:50]
        ]
    }
    return result, time_ms, rms, rolling_baseline, rolling_mean, rms_slope

def main():
    onset_file = "processing/haptic_sources/f1_2025/f1_onsets.csv"
    rms_file = "processing/haptic_sources/f1_2025/f1_rms.csv"
    
    onset_res, onset_ms, onset_sec = analyze_onsets(onset_file)
    rms_res, time_ms, rms, rolling_baseline, rolling_mean, rms_slope = analyze_rms(rms_file)
    
    with open("processing/output/f1_2025/f1_onset_analysis.json", "w") as f:
        json.dump(onset_res, f, indent=2)
        
    with open("processing/output/f1_2025/f1_rms_analysis.json", "w") as f:
        json.dump(rms_res, f, indent=2)
        
    print(f"Onsets analysis saved. Total onsets: {onset_res['n_rows']}")
    print(f"RMS analysis saved. Total samples: {rms_res['n_rows']}, max RMS: {rms_res['rms_max']:.6f}")

if __name__ == "__main__":
    main()
