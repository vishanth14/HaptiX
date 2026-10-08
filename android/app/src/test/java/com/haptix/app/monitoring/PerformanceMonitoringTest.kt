package com.haptix.app.monitoring

import android.content.Intent
import android.os.BatteryManager
import com.haptix.app.data.model.PerformanceMetric
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.haptics.HapticEngine
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.ui.screens.video.VideoPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceMonitoringTest {

    // Helper fake collectors for testing
    private class FakeCpuCollector(var usage: Float? = null) : CpuCollector {
        override fun sampleCpuUsage(): Float? = usage
    }

    private class FakeGpuCollector(var usage: Float? = null) : GpuCollector {
        override fun sampleGpuUsage(): Float? = usage
    }

    private class FakeTemperatureCollector(var temp: Float? = null) : TemperatureCollector {
        override fun sampleTemperature(): Float? = temp
    }

    private class FakeHapticEngine(private val supported: Boolean = true) : HapticEngine {
        var isPlaying: Boolean = false
        var loadedPattern: com.haptix.app.data.model.HapticPattern? = null
        val playedEvents = mutableListOf<com.haptix.app.data.model.HapticEvent>()

        override fun isHapticSupported(): Boolean = supported
        override fun loadPattern(pattern: com.haptix.app.data.model.HapticPattern) { loadedPattern = pattern }
        override fun start() { isPlaying = true }
        override fun stop() { isPlaying = false }
        override fun release() { isPlaying = false; loadedPattern = null }
        override fun playEvent(event: com.haptix.app.data.model.HapticEvent) { playedEvents.add(event) }
    }

    // =========================================================================
    // 1. PerformanceMetric Model Tests
    // =========================================================================

    @Test
    fun performanceMetric_supportsNullableValuesByDefault() {
        val metric = PerformanceMetric(
            timestamp = 1700000000000L,
            videoId = "video_f1"
        )

        assertNull("cpuUsagePercent must default to null when unavailable", metric.cpuUsagePercent)
        assertNull("gpuUsagePercent must default to null when unavailable", metric.gpuUsagePercent)
        assertNull("deviceTemperatureCelsius must default to null when unavailable", metric.deviceTemperatureCelsius)
        assertNull("playbackPositionMs must default to null when not specified", metric.playbackPositionMs)
        assertEquals("video_f1", metric.videoId)
        assertEquals(1700000000000L, metric.timestamp)
    }

    @Test
    fun performanceMetric_retainsAccurateMeasurementsAndPlaybackPosition() {
        val metric = PerformanceMetric(
            timestamp = 1700000000000L,
            cpuUsagePercent = 28.5f,
            gpuUsagePercent = 15.0f,
            deviceTemperatureCelsius = 36.2f,
            videoId = "video_koji",
            playbackPositionMs = 4500L
        )

        assertNotNull(metric.cpuUsagePercent)
        assertEquals(28.5f, metric.cpuUsagePercent!!, 0.01f)
        assertNotNull(metric.gpuUsagePercent)
        assertEquals(15.0f, metric.gpuUsagePercent!!, 0.01f)
        assertNotNull(metric.deviceTemperatureCelsius)
        assertEquals(36.2f, metric.deviceTemperatureCelsius!!, 0.01f)
        assertEquals(4500L, metric.playbackPositionMs)
        assertEquals("video_koji", metric.videoId)
    }

    // =========================================================================
    // 2. CPU Collector Tests
    // =========================================================================

    @Test
    fun cpuCollector_baselineReturnsNullAndCalculatesCpuDelta() {
        var simulatedCpuTimeMs = 1000L
        var simulatedClockTimeMs = 10_000L

        val collector = AndroidCpuCollector(
            availableCores = 4,
            processCpuTimeProvider = { simulatedCpuTimeMs },
            clockTimeProvider = { simulatedClockTimeMs }
        )

        // First call is baseline: cannot compute delta yet
        val baseline = collector.sampleCpuUsage()
        assertNull("First CPU reading must be null (establishes baseline)", baseline)

        // Advance 1000ms wall time, during which process used 2000ms CPU time across 4 cores
        // Expected CPU % = (2000 / (1000 * 4)) * 100 = 50.0%
        simulatedCpuTimeMs += 2000L
        simulatedClockTimeMs += 1000L

        val reading = collector.sampleCpuUsage()
        assertNotNull("Subsequent CPU reading must be non-null", reading)
        assertEquals(50.0f, reading!!, 0.1f)
    }

    @Test
    fun cpuCollector_handlesUnavailableProcessMetricsGracefully() {
        val collector = AndroidCpuCollector(
            availableCores = 4,
            processCpuTimeProvider = { -1L }, // Negative indicates error / unavailable
            clockTimeProvider = { 10_000L }
        )

        val reading = collector.sampleCpuUsage()
        assertNull("CPU collector must return null when process CPU time is unavailable", reading)
    }

    @Test
    fun cpuCollector_handlesZeroDeltaTimeGracefully() {
        var simulatedCpuTimeMs = 1000L
        val simulatedClockTimeMs = 10_000L

        val collector = AndroidCpuCollector(
            availableCores = 4,
            processCpuTimeProvider = { simulatedCpuTimeMs },
            clockTimeProvider = { simulatedClockTimeMs }
        )

        collector.sampleCpuUsage() // baseline
        simulatedCpuTimeMs += 100L // CPU changed, but clock did not

        val reading = collector.sampleCpuUsage()
        assertNull("CPU collector must return null when deltaTime is zero", reading)
    }

    // =========================================================================
    // 3. GPU Collector Tests
    // =========================================================================

    @Test
    fun gpuCollector_returnsNullWhenSysfsUnavailable() {
        val collector = AndroidGpuCollector(
            fileReader = { null }
        )

        val reading = collector.sampleGpuUsage()
        assertNull("GPU collector must return null when sysfs nodes are unavailable", reading)
    }

    @Test
    fun gpuCollector_parsesStandardNumericPercentageWhenAvailable() {
        val collector = AndroidGpuCollector(
            fileReader = { path ->
                if (path.contains("gpu_busy_percentage")) "43.5%" else null
            }
        )

        val reading = collector.sampleGpuUsage()
        assertNotNull(reading)
        assertEquals(43.5f, reading!!, 0.01f)
    }

    @Test
    fun gpuCollector_parsesKgslBusyCyclesFormat() {
        val collector = AndroidGpuCollector(
            fileReader = { path ->
                if (path.contains("gpubusy")) "250 1000" else null
            }
        )

        val reading = collector.sampleGpuUsage()
        assertNotNull(reading)
        assertEquals(25.0f, reading!!, 0.01f)
    }

    @Test
    fun gpuCollector_returnsNullOnCorruptContent() {
        val collector = AndroidGpuCollector(
            fileReader = { "NOT_A_NUMBER" }
        )

        val reading = collector.sampleGpuUsage()
        assertNull("GPU collector must return null on unparseable content", reading)
    }

    // =========================================================================
    // 4. Temperature Collector Tests
    // =========================================================================

    @Test
    fun temperatureCollector_returnsNullWhenSensorsUnavailable() {
        val collector = AndroidTemperatureCollector(
            batteryIntentProvider = { null },
            thermalFileReader = { null }
        )

        val reading = collector.sampleTemperature()
        assertNull("Temperature collector must return null when sensors are unavailable", reading)
    }

    @Test
    fun temperatureCollector_readsFromThermalZoneWhenBatteryUnavailable() {
        val collector = AndroidTemperatureCollector(
            batteryIntentProvider = { null },
            thermalFileReader = { path ->
                if (path.contains("thermal_zone0")) "38500" else null // 38,500 millidegrees C
            }
        )

        val reading = collector.sampleTemperature()
        assertNotNull(reading)
        assertEquals(38.5f, reading!!, 0.01f)
    }

    // =========================================================================
    // 5. PerformanceMonitor Lifecycle & 2-Second Sampling Logic
    // =========================================================================

    @Test
    fun performanceMonitor_initialStateIsNotMonitoring() {
        val monitor = PerformanceMonitor()
        assertFalse(monitor.isMonitoring)
        assertFalse(monitor.isPaused)
        assertTrue(monitor.getRecordedMetrics().isEmpty())
    }

    @Test
    fun performanceMonitor_sampleNowCapturesAllFields() {
        val fakeCpu = FakeCpuCollector(32.0f)
        val fakeGpu = FakeGpuCollector(null) // GPU unavailable on device
        val fakeTemp = FakeTemperatureCollector(35.5f)

        var currentPos = 2400L

        val monitor = PerformanceMonitor(
            cpuCollector = fakeCpu,
            gpuCollector = fakeGpu,
            temperatureCollector = fakeTemp
        )

        monitor.startMonitoring("stimulus_alpha") { currentPos }

        val snapshot = monitor.sampleNow()
        assertEquals("stimulus_alpha", snapshot.videoId)
        assertEquals(2400L, snapshot.playbackPositionMs)
        assertNotNull(snapshot.cpuUsagePercent)
        assertEquals(32.0f, snapshot.cpuUsagePercent!!, 0.01f)
        assertNull("GPU must be null when unavailable", snapshot.gpuUsagePercent)
        assertNotNull(snapshot.deviceTemperatureCelsius)
        assertEquals(35.5f, snapshot.deviceTemperatureCelsius!!, 0.01f)

        assertEquals(1, monitor.getRecordedMetrics().size)
    }

    @Test
    fun performanceMonitor_lifecycleStartPauseResumeStop() = runBlocking {
        val fakeCpu = FakeCpuCollector(20.0f)
        val fakeGpu = FakeGpuCollector(null)
        val fakeTemp = FakeTemperatureCollector(31.0f)

        var playbackPosition = 1000L
        val testScope = CoroutineScope(Dispatchers.Default)

        // Use 40ms interval for fast unit test execution
        val monitor = PerformanceMonitor(
            cpuCollector = fakeCpu,
            gpuCollector = fakeGpu,
            temperatureCollector = fakeTemp,
            coroutineScope = testScope,
            samplingIntervalMs = 40L
        )

        // 1. Start monitoring
        monitor.startMonitoring("test_video") { playbackPosition }
        assertTrue(monitor.isMonitoring)
        assertFalse(monitor.isPaused)

        // Wait for ~2 samples (80-100ms)
        delay(110L)
        val countAfterStart = monitor.getRecordedMetrics().size
        assertTrue("Expected at least 1 sample after starting, got $countAfterStart", countAfterStart >= 1)

        // 2. Pause monitoring
        monitor.pauseMonitoring()
        assertTrue(monitor.isPaused)
        val countAtPause = monitor.getRecordedMetrics().size

        // Wait while paused: should NOT accumulate new samples
        delay(100L)
        val countAfterPauseWait = monitor.getRecordedMetrics().size
        assertEquals("No samples should be collected while paused", countAtPause, countAfterPauseWait)

        // 3. Resume monitoring
        playbackPosition = 5000L
        monitor.resumeMonitoring()
        assertFalse(monitor.isPaused)

        // Wait for more samples
        delay(110L)

        // 4. Stop monitoring
        val finalMetrics = monitor.stopMonitoring()
        assertFalse(monitor.isMonitoring)
        assertFalse(monitor.isPaused)
        assertTrue("Expected more samples after resume, got ${finalMetrics.size}", finalMetrics.size > countAtPause)
        assertEquals(finalMetrics.size, monitor.getRecordedMetrics().size)

        // Wait after stop: should NOT accumulate new samples
        delay(100L)
        assertEquals("No samples should be collected after stop", finalMetrics.size, monitor.getRecordedMetrics().size)
    }

    // =========================================================================
    // 6. VideoPlayerViewModel Performance Integration
    // =========================================================================

    @Test
    fun videoPlayerViewModel_controlsPerformanceMonitorLifecycle() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeEngine = FakeHapticEngine()
        val synchronizer = HapticSynchronizer(engine = fakeEngine)
        val fakeCpu = FakeCpuCollector(18.0f)
        val fakeGpu = FakeGpuCollector(null)
        val fakeTemp = FakeTemperatureCollector(33.0f)

        val monitor = PerformanceMonitor(
            cpuCollector = fakeCpu,
            gpuCollector = fakeGpu,
            temperatureCollector = fakeTemp,
            coroutineScope = testScope
        )

        val viewModel = VideoPlayerViewModel(
            videoRepository = DefaultVideoRepository(),
            hapticRepository = DefaultHapticRepository(),
            hapticSynchronizer = synchronizer,
            performanceMonitor = monitor,
            hapticEngine = fakeEngine,
            coroutineScope = testScope
        )

        // Load video: monitor is started but paused until playback begins
        viewModel.loadVideo("video_01")
        assertTrue(monitor.isMonitoring)
        assertTrue("Monitor should be paused while video is not yet playing", monitor.isPaused)

        // Play video: monitor resumes
        viewModel.play()
        assertFalse("Monitor should resume when playback begins", monitor.isPaused)

        // Pause video: monitor pauses
        viewModel.pause()
        assertTrue("Monitor should pause when playback pauses", monitor.isPaused)

        // Resume video
        viewModel.play()
        assertFalse(monitor.isPaused)

        // Video completes
        viewModel.updatePlaybackState(isPlaying = false, isCompleted = true)
        assertFalse("Monitor should stop when playback completes", monitor.isMonitoring)

        // Metrics are retained in memory
        val recorded = viewModel.getRecordedPerformanceMetrics()
        assertNotNull(recorded)
    }
}
