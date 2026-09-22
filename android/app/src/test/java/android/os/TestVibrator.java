package android.os;

/**
 * Test double for [android.os.Vibrator] residing in the android.os package
 * to safely access the package-private default constructor on JVM test runners.
 */
public class TestVibrator extends Vibrator {
    private final boolean hasVib;
    private final boolean hasAmp;
    private final boolean envelopeSupported;
    private final float resonantFreq;
    public int cancelCalls = 0;
    public int vibrateCalls = 0;

    public TestVibrator(boolean hasVib, boolean hasAmp, boolean envelopeSupported, float resonantFreq) {
        this.hasVib = hasVib;
        this.hasAmp = hasAmp;
        this.envelopeSupported = envelopeSupported;
        this.resonantFreq = resonantFreq;
    }

    @Override
    public boolean hasVibrator() {
        return hasVib;
    }

    @Override
    public boolean hasAmplitudeControl() {
        return hasAmp;
    }

    public boolean areEnvelopeEffectsSupported() {
        return envelopeSupported;
    }

    public float getResonantFrequency() {
        return resonantFreq;
    }

    @Override
    public void cancel() {
        cancelCalls++;
    }

    @Override
    public void vibrate(long milliseconds) {
        vibrateCalls++;
    }

    @Override
    public void vibrate(long[] pattern, int repeat) {
        vibrateCalls++;
    }
}
