import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np
import pandas as pd

def generate_visualization():
    # 1. Load data
    df_rms = pd.read_csv("processing/haptic_sources/f1_2025/f1_rms.csv", header=None, names=['time_sec', 'rms'])
    time_sec = df_rms['time_sec'].values
    rms = df_rms['rms'].values
    
    with open("processing/haptic_sources/f1_2025/f1_onsets.csv", "r") as f:
        onset_sec = np.array([float(x.strip()) for x in f if x.strip()])
        
    with open("android/app/src/main/assets/haptics/f1_2025_haptic_representation_final.json", "r") as f:
        final_data = json.load(f)
    events = final_data['events']
    
    # Color palette for semantic types
    color_map = {
        'CINEMATIC_ACCENT': '#9C27B0',    # Purple
        'LOW_FREQUENCY_DRONE': '#607D8B', # Blue Grey
        'ENGINE_RUMBLE': '#FF9800',       # Orange
        'GEAR_SHIFT': '#E91E63',          # Pink/Red transient
        'ACCELERATION_RISE': '#FF5722',   # Deep Orange
        'TRANSITION': '#795548',          # Brown
        'LIGHT_IMPACT': '#03A9F4',        # Light Blue
        'CURB_RUMBLE': '#009688',         # Teal
        'CONTINUOUS_RUMBLE': '#F44336',   # Red
        'FAST_MOTION': '#00BCD4',         # Cyan
        'TENSION_BUILD': '#FFC107',       # Amber
        'COLLISION': '#D50000',           # Crimson
        'CRASH_AFTERSHOCK': '#7E57C2',    # Deep Purple
        'HEAVY_IMPACT': '#C2185B'         # Magenta
    }
    
    fig = plt.figure(figsize=(18, 12), dpi=200)
    plt.subplots_adjust(hspace=0.35)
    
    # -------------------------------------------------------------
    # Panel 1: Full 130.54s Timeline
    # -------------------------------------------------------------
    ax1 = plt.subplot(3, 1, 1)
    ax1.set_title("HaptiX F1 2025 Final Haptic Soundtrack — Complete Perceptual Timeline (0 – 130.54 s)", fontsize=13, fontweight='bold', pad=10)
    
    # RMS curve
    ax1.plot(time_sec, rms, color='#78909C', alpha=0.5, linewidth=0.8, label="Audio RMS")
    
    # Onsets
    ax1.scatter(onset_sec, np.full_like(onset_sec, -0.006), color='#455A64', s=4, alpha=0.6, marker='|', label="Audio Onsets")
    
    # Event regions
    for ev in events:
        s = ev['startTimeMs'] / 1000.0
        dur = ev['durationMs'] / 1000.0
        intensity = ev['intensity']
        sem = ev['semanticType']
        c = color_map.get(sem, '#3F51B5')
        
        # Draw block
        rect = patches.Rectangle((s, 0), dur, intensity * 0.1, linewidth=1, edgecolor=c, facecolor=c, alpha=0.7)
        ax1.add_patch(rect)
        
    ax1.set_xlim(0, 130.54)
    ax1.set_ylim(-0.01, 0.12)
    ax1.set_xlabel("Timeline (seconds)", fontsize=10)
    ax1.set_ylabel("Audio RMS & Haptic Int", fontsize=10)
    ax1.grid(True, linestyle=':', alpha=0.5)
    
    # Highlight silence vs active
    # Draw silence indicator bar at y = 0.115
    cur_end = 0.0
    for ev in events:
        s = ev['startTimeMs'] / 1000.0
        if s > cur_end:
            ax1.plot([cur_end, s], [0.113, 0.113], color='#B0BEC5', linewidth=3)
        ax1.plot([s, s + ev['durationMs']/1000.0], [0.113, 0.113], color='#2E7D32', linewidth=4)
        cur_end = s + ev['durationMs'] / 1000.0
    if cur_end < 130.54:
        ax1.plot([cur_end, 130.54], [0.113, 0.113], color='#B0BEC5', linewidth=3)
        
    # Legend
    legend_elements = [
        patches.Patch(facecolor='#2E7D32', label='Active Haptic Events (15.5%)'),
        patches.Patch(facecolor='#B0BEC5', label='Tactile Silence / Actuator Rest (84.5%)'),
        patches.Patch(facecolor='#D50000', label='Crash Collision (105.06s, int=0.744)'),
        patches.Patch(facecolor='#C2185B', label='Climax Beats (123.5s - 126.0s)')
    ]
    ax1.legend(handles=legend_elements, loc='upper right', fontsize=9, framealpha=0.9)

    # -------------------------------------------------------------
    # Panel 2: Zoom on Acceleration & Track Action (24s to 46s)
    # -------------------------------------------------------------
    ax2 = plt.subplot(3, 1, 2)
    ax2.set_title("Sector Focus: Upshifts, Slipstream Overtake & Curb Strikes (24.0 s – 46.0 s)", fontsize=12, fontweight='bold', pad=10)
    
    mask_sec2 = (time_sec >= 24.0) & (time_sec <= 46.0)
    ax2.plot(time_sec[mask_sec2], rms[mask_sec2], color='#546E7A', linewidth=1.2, label="Audio RMS")
    
    onsets_sec2 = onset_sec[(onset_sec >= 24.0) & (onset_sec <= 46.0)]
    ax2.scatter(onsets_sec2, np.full_like(onsets_sec2, -0.005), color='#37474F', s=15, marker='|', label="Audio Onsets")
    
    for ev in events:
        s = ev['startTimeMs'] / 1000.0
        dur = ev['durationMs'] / 1000.0
        end = s + dur
        if end >= 24.0 and s <= 46.0:
            intensity = ev['intensity']
            sem = ev['semanticType']
            c = color_map.get(sem, '#3F51B5')
            rect = patches.Rectangle((s, 0), dur, intensity * 0.1, linewidth=1.5, edgecolor=c, facecolor=c, alpha=0.65)
            ax2.add_patch(rect)
            
            # Label
            p_time = ev['peakTimeMs'] / 1000.0
            ax2.text(p_time, intensity * 0.1 + 0.005, f"{sem}\n({intensity:.2f})", fontsize=7.5, ha='center', va='bottom', weight='bold', color=c)
            
    ax2.set_xlim(24.0, 46.0)
    ax2.set_ylim(-0.008, 0.12)
    ax2.set_xlabel("Timeline (seconds)", fontsize=10)
    ax2.set_ylabel("Audio RMS & Haptic Int", fontsize=10)
    ax2.grid(True, linestyle=':', alpha=0.5)

    # -------------------------------------------------------------
    # Panel 3: Zoom on Crash & Climax Hits (100s to 128s)
    # -------------------------------------------------------------
    ax3 = plt.subplot(3, 1, 3)
    ax3.set_title("Climax Focus: Major Collision Impact & Six-Beat Percussive Sting (100.0 s – 128.0 s)", fontsize=12, fontweight='bold', pad=10)
    
    mask_sec3 = (time_sec >= 100.0) & (time_sec <= 128.0)
    ax3.plot(time_sec[mask_sec3], rms[mask_sec3], color='#546E7A', linewidth=1.2, label="Audio RMS")
    
    onsets_sec3 = onset_sec[(onset_sec >= 100.0) & (onset_sec <= 128.0)]
    ax3.scatter(onsets_sec3, np.full_like(onsets_sec3, -0.005), color='#37474F', s=15, marker='|', label="Audio Onsets")
    
    for ev in events:
        s = ev['startTimeMs'] / 1000.0
        dur = ev['durationMs'] / 1000.0
        end = s + dur
        if end >= 100.0 and s <= 128.0:
            intensity = ev['intensity']
            sem = ev['semanticType']
            c = color_map.get(sem, '#3F51B5')
            rect = patches.Rectangle((s, 0), dur, intensity * 0.1, linewidth=1.5, edgecolor=c, facecolor=c, alpha=0.65)
            ax3.add_patch(rect)
            
            p_time = ev['peakTimeMs'] / 1000.0
            ax3.text(p_time, intensity * 0.1 + 0.005, f"{sem}\n({intensity:.2f})", fontsize=7.5, ha='center', va='bottom', weight='bold', color=c)
            
    ax3.set_xlim(100.0, 128.0)
    ax3.set_ylim(-0.008, 0.12)
    ax3.set_xlabel("Timeline (seconds)", fontsize=10)
    ax3.set_ylabel("Audio RMS & Haptic Int", fontsize=10)
    ax3.grid(True, linestyle=':', alpha=0.5)

    plt.tight_layout()
    out_png = "processing/output/f1_2025/f1_2025_haptic_representation_final_timeline.png"
    plt.savefig(out_png, dpi=200)
    plt.close()
    print(f"Generated timeline visualization at {out_png}")

if __name__ == "__main__":
    generate_visualization()
