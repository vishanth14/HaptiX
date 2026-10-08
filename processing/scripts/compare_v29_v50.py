import json

def inspect_events():
    with open('android/app/src/main/assets/haptics/f1_2025_haptic_trailer_haptic_representation_v5.json', 'r') as f:
        v29 = json.load(f)
    with open('android/app/src/main/assets/haptics/f1_2025_haptic_representation_v5_BACKUP.json', 'r') as f:
        v50 = json.load(f)
        
    print(f"v29 has {len(v29['events'])} events:")
    for e in v29['events']:
        p_ms = e.get('peakTimeMs', e['startTimeMs'])
        print(f"  {e['id']}: {e['startTimeMs']:6d}ms - {e['startTimeMs']+e['durationMs']:6d}ms (dur={e['durationMs']:4d}ms, peak={p_ms:6d}ms) | {e['semanticType']:<20} | int={e['intensity']:.3f} | {e.get('type')}")
        
    print(f"\nv50 has {len(v50['events'])} events:")
    for e in v50['events']:
        p_ms = e.get('peakTimeMs', e['startTimeMs'])
        print(f"  {e['id']}: {e['startTimeMs']:6d}ms - {e['startTimeMs']+e['durationMs']:6d}ms (dur={e['durationMs']:4d}ms, peak={p_ms:6d}ms) | {e['semanticType']:<20} | int={e['intensity']:.3f} | {e.get('type')}")

if __name__ == '__main__':
    inspect_events()
