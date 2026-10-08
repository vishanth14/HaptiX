package com.haptix.app.data.model

/**
 * Semantic classification vocabulary describing the physical and cinematic sensation
 * of a haptic event, decoupling tactile intent from platform-specific vibration APIs.
 */
enum class SemanticHapticType {
    /**
     * Subtle, continuous sensation with gradual fade-in and gradual fade-out.
     */
    SMOOTH,

    /**
     * Tension or motion sensation gradually building up from near-zero to target peak.
     */
    SMOOTH_BUILD,

    /**
     * Sensation fading away smoothly after an event or action.
     */
    SMOOTH_RELEASE,

    /**
     * Powerful physical contact with a short controlled attack, strong body, and natural decay.
     */
    HEAVY_IMPACT,

    /**
     * Lighter physical contact, small object movement, or gentle landing with low/moderate peak.
     */
    SOFT_IMPACT,

    /**
     * Continuous environmental or ground vibration with smooth fade-in and fade-out.
     */
    RUMBLE,

    /**
     * Gradual, suspenseful build with slow modulation and gentle release.
     */
    TENSION,

    /**
     * Cinematic, very low-intensity swell with slow fade-in and long fade-out.
     */
    EMOTIONAL_SWELL,

    /**
     * Directional kinetic movement (e.g. camera pan, weapon swing) with smooth ramp up and down.
     */
    SWEEP,

    /**
     * Strong power surge with controlled build, peak body, and smooth decay.
     */
    ENERGY,

    /**
     * Sharp metallic or solid collision with rapid controlled attack, crisp body, and smooth release.
     */
    METALLIC_CLASH,

    /**
     * Two distinct pulses separated by a brief gap, each with independent smooth envelopes.
     */
    DOUBLE_PULSE,

    /**
     * Rapid sequence of micro-pulses, each avoiding abrupt instantaneous spikes.
     */
    RAPID_PULSES,

    /**
     * Multi-phase kinematic jump trajectory: anticipation build -> launch ascent -> airborne apex -> controlled landing impact.
     */
    JUMP,

    /**
     * Upward acceleration or launch surge building into airborne release.
     */
    LAUNCH,

    // ==========================================
    // CANONICAL HAPTIX V2 SENSATION VOCABULARY
    // ==========================================

    CONTINUOUS_RUMBLE,
    LOW_FREQUENCY_DRONE,
    ENGINE_RUMBLE,
    ACCELERATION_RISE,
    BRAKING_PRESSURE,
    FAST_MOTION,
    ROAD_TEXTURE,
    CURB_RUMBLE,
    RUNNING_RHYTHM,
    MECHANICAL_TEXTURE,
    MECHANICAL_CLICK,
    GEAR_SHIFT,
    LIGHT_IMPACT,
    COLLISION,
    CRASH_AFTERSHOCK,
    LANDING,
    FALL,
    TENSION_BUILD,
    CINEMATIC_ACCENT,
    TRANSITION,
    QUIET;

    companion object {
        /**
         * Safely parses a string identifier into a [SemanticHapticType], falling back to [SMOOTH]
         * or inferring from legacy and canonical descriptors.
         */
        fun fromString(value: String?): SemanticHapticType {
            if (value.isNullOrBlank()) return SMOOTH
            val normalized = value.trim().uppercase()
            return entries.find { it.name == normalized } ?: when {
                normalized.contains("QUIET") || normalized.contains("SILENCE") -> QUIET
                normalized.contains("AFTERSHOCK") -> CRASH_AFTERSHOCK
                normalized.contains("COLLISION") || normalized.contains("CRASH") -> COLLISION
                normalized.contains("LANDING") -> LANDING
                normalized.contains("FALL") -> FALL
                normalized.contains("JUMP") || normalized.contains("LEAP") || normalized.contains("VAULT") -> JUMP
                normalized.contains("LAUNCH") || normalized.contains("TAKEOFF") -> LAUNCH
                normalized.contains("ACCELERATION") || normalized.contains("THROTTLE") -> ACCELERATION_RISE
                normalized.contains("BRAK") -> BRAKING_PRESSURE
                normalized.contains("GEAR") || normalized.contains("SHIFT") -> GEAR_SHIFT
                normalized.contains("CLICK") -> MECHANICAL_CLICK
                normalized.contains("MECHANICAL") -> MECHANICAL_TEXTURE
                normalized.contains("CURB") || normalized.contains("KERB") -> CURB_RUMBLE
                normalized.contains("ROAD") -> ROAD_TEXTURE
                normalized.contains("ENGINE") -> ENGINE_RUMBLE
                normalized.contains("DRONE") -> LOW_FREQUENCY_DRONE
                normalized.contains("RUNNING") || normalized.contains("FOOTSTEP") -> RUNNING_RHYTHM
                normalized.contains("FAST") || normalized.contains("FLYBY") -> FAST_MOTION
                normalized.contains("TRANSITION") -> TRANSITION
                normalized.contains("ACCENT") -> CINEMATIC_ACCENT
                normalized.contains("HEAVY") || normalized.contains("SLAM") || normalized.contains("BURST") -> HEAVY_IMPACT
                normalized.contains("SOFT") || normalized.contains("LIGHT") || normalized.contains("TAP") -> LIGHT_IMPACT
                normalized.contains("RUMBLE") || normalized.contains("SHOCKWAVE") || normalized.contains("GROUND") -> CONTINUOUS_RUMBLE
                normalized.contains("TENSION") || normalized.contains("SUSPENSE") -> TENSION_BUILD
                normalized.contains("EMOTIONAL") || normalized.contains("SWELL") -> EMOTIONAL_SWELL
                normalized.contains("SWEEP") || normalized.contains("SWING") || normalized.contains("DASH") -> SWEEP
                normalized.contains("ENERGY") || normalized.contains("POWER") || normalized.contains("SURGE") -> ENERGY
                normalized.contains("CLASH") || normalized.contains("METALLIC") || normalized.contains("BLADE") -> METALLIC_CLASH
                normalized.contains("DOUBLE") -> DOUBLE_PULSE
                normalized.contains("RAPID") -> RAPID_PULSES
                normalized.contains("BUILD") -> SMOOTH_BUILD
                normalized.contains("RELEASE") -> SMOOTH_RELEASE
                else -> SMOOTH
            }
        }
    }
}
