import json
import numpy as np

def run_validation():
    json_path = "android/app/src/main/assets/haptics/f1_2025_haptic_representation_final.json"
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    video_duration_ms = data.get("videoDurationMs", 130540)
    events = data.get("events", [])
    
    # 1. Integrity checks
    assert len(events) > 0, "No events found"
    
    ids = [e["id"] for e in events]
    duplicate_ids = len(ids) - len(set(ids))
    assert duplicate_ids == 0, f"Found {duplicate_ids} duplicate IDs"
    
    # Chronological ordering and boundaries
    overlaps = 0
    envelope_mismatches = 0
    out_of_bounds = 0
    micro_events = 0 # duration < 80ms
    
    prev_end = 0
    active_duration_ms = 0
    silence_intervals = []
    
    for i, e in enumerate(events):
        s = e["startTimeMs"]
        d = e["durationMs"]
        end = s + d
        att = e["attackMs"]
        sus = e["sustainMs"]
        rel = e["releaseMs"]
        
        # Check start < end
        assert s >= 0, f"Event {e['id']} negative start: {s}"
        assert d > 0, f"Event {e['id']} zero or negative duration: {d}"
        assert end <= video_duration_ms, f"Event {e['id']} exceeds video duration: {end} > {video_duration_ms}"
        
        # Micro events check
        if d < 80:
            micro_events += 1
            
        # Envelope check
        if att + sus + rel != d:
            envelope_mismatches += 1
            
        # Overlap check
        if s < prev_end:
            overlaps += 1
            
        # Silence interval before this event
        if s > prev_end:
            silence_intervals.append({
                "startMs": prev_end,
                "endMs": s,
                "durationMs": s - prev_end
            })
            
        active_duration_ms += d
        prev_end = end
        
    # Final silence interval after last event
    if prev_end < video_duration_ms:
        silence_intervals.append({
            "startMs": prev_end,
            "endMs": video_duration_ms,
            "durationMs": video_duration_ms - prev_end
        })
        
    silence_duration_ms = sum(x["durationMs"] for x in silence_intervals)
    assert active_duration_ms + silence_duration_ms == video_duration_ms, f"Timeline mismatch: {active_duration_ms} + {silence_duration_ms} != {video_duration_ms}"
    
    # Counts
    total_events = len(events)
    transient_events = sum(1 for e in events if e.get("type") == "TRANSIENT")
    continuous_events = sum(1 for e in events if e.get("type") == "CONTINUOUS")
    
    active_pct = (active_duration_ms / video_duration_ms) * 100.0
    silence_pct = (silence_duration_ms / video_duration_ms) * 100.0
    
    intensities = [e["intensity"] for e in events]
    durations = [e["durationMs"] for e in events]
    
    # Density per minute (video is 130.54s = 2.1757 minutes)
    video_minutes = video_duration_ms / 60000.0
    density_per_min = total_events / video_minutes
    
    # Landmark timing check
    checkpoints = [
        26640, 27140, 29900, 30760, 32640,
        42860, 44360, 55620, 73880, 80240,
        105060, 117520, 123520, 124020, 124520,
        125020, 125520, 126020
    ]
    checkpoint_errors = []
    for cp in checkpoints:
        # Match event
        ev = next((e for e in events if e.get("peakTimeMs") == cp or e["startTimeMs"] <= cp <= e["startTimeMs"] + e["durationMs"]), None)
        if ev:
            err = abs(ev.get("peakTimeMs", ev["startTimeMs"]) - cp)
            checkpoint_errors.append({"checkpointMs": cp, "eventId": ev["id"], "peakTimeMs": ev.get("peakTimeMs"), "errorMs": err})
        else:
            checkpoint_errors.append({"checkpointMs": cp, "eventId": None, "errorMs": None})
            
    metrics = {
        "videoId": data["videoId"],
        "version": data["version"],
        "videoDurationMs": video_duration_ms,
        "totalEvents": total_events,
        "transientEvents": transient_events,
        "continuousEvents": continuous_events,
        "activeDurationMs": active_duration_ms,
        "silenceDurationMs": silence_duration_ms,
        "activePercentage": round(active_pct, 4),
        "silencePercentage": round(silence_pct, 4),
        "minIntensity": float(np.min(intensities)),
        "maxIntensity": float(np.max(intensities)),
        "meanIntensity": float(np.mean(intensities)),
        "medianIntensity": float(np.median(intensities)),
        "minDurationMs": int(np.min(durations)),
        "maxDurationMs": int(np.max(durations)),
        "meanDurationMs": float(np.mean(durations)),
        "eventDensityPerMinute": round(density_per_min, 2),
        "numberOfOverlaps": overlaps,
        "numberOfDuplicateIds": duplicate_ids,
        "envelopeMismatches": envelope_mismatches,
        "microEventsCount": micro_events,
        "silenceIntervalsCount": len(silence_intervals),
        "maxCheckpointTimingErrorMs": max(c["errorMs"] for c in checkpoint_errors if c["errorMs"] is not None),
        "checkpointErrors": checkpoint_errors
    }
    
    with open("processing/output/f1_2025/f1_final_validation_metrics.json", "w") as f:
        json.dump(metrics, f, indent=2)
        
    print(f"Validation successful! Metrics:")
    print(f"  Total Events: {total_events} (Transient: {transient_events}, Continuous: {continuous_events})")
    print(f"  Active Duration: {active_duration_ms} ms ({active_pct:.2f}%)")
    print(f"  Silence Duration: {silence_duration_ms} ms ({silence_pct:.2f}%)")
    print(f"  Sum: {active_duration_ms + silence_duration_ms} ms == {video_duration_ms} ms")
    print(f"  Intensity: min={metrics['minIntensity']:.3f}, max={metrics['maxIntensity']:.3f}, mean={metrics['meanIntensity']:.3f}")
    print(f"  Duration: min={metrics['minDurationMs']} ms, max={metrics['maxDurationMs']} ms, mean={metrics['meanDurationMs']:.1f} ms")
    print(f"  Overlaps: {overlaps}, Duplicate IDs: {duplicate_ids}, Envelope Mismatches: {envelope_mismatches}")
    print(f"  Max Checkpoint Timing Error: {metrics['maxCheckpointTimingErrorMs']} ms")

if __name__ == "__main__":
    run_validation()
