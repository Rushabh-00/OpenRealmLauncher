package dev.openrealm.launcher.utils.device

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.os.Process
import android.os.SystemClock
import dev.openrealm.launcher.game.renderer.Renderers
import dev.openrealm.launcher.game.version.installed.Version
import dev.openrealm.launcher.ui.screens.game.elements.GamePerformanceStats
import kotlin.math.roundToInt

class GamePerformanceSampler(
    private val context: Context,
    private val version: Version
) {
    private companion object {
        const val BYTES_PER_MB = 1024L * 1024L
        const val MEMORY_SAMPLE_INTERVAL_MS = 1000L
    }

    private val runtime = Runtime.getRuntime()
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()

    private var lastCpuTimeMs = Process.getElapsedCpuTime()
    private var lastWallTimeMs = SystemClock.elapsedRealtime()
    private var lastMemorySampleMs = -MEMORY_SAMPLE_INTERVAL_MS
    private var cachedMemoryUsedMb = 0
    private var cachedMemoryTotalMb = 0

    private val rendererName by lazy {
        runCatching { Renderers.getCurrentRenderer().getRendererName() }.getOrDefault("N/A")
    }

    private val graphicsApiName by lazy {
        runCatching {
            version.getGraphicsApi().displayName.ifBlank { "N/A" }
        }.getOrDefault("N/A")
    }

    private fun readDisplayRefreshRate(): Int? {
        return runCatching {
            val displayManager = context.getSystemService(DisplayManager::class.java)
            val refreshRate = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: 0f
            refreshRate.takeIf { it.isFinite() && it > 0f }?.roundToInt()
        }.getOrNull()
    }

    fun sample(fps: Int): GamePerformanceStats {
        val nowCpu = Process.getElapsedCpuTime()
        val nowWall = SystemClock.elapsedRealtime()
        val cpuDelta = (nowCpu - lastCpuTimeMs).coerceAtLeast(0L)
        val wallDelta = (nowWall - lastWallTimeMs).coerceAtLeast(1L)
        lastCpuTimeMs = nowCpu
        lastWallTimeMs = nowWall

        val cpuPercent = (
            cpuDelta.toDouble() /
                (wallDelta.toDouble() * runtime.availableProcessors()) *
                100.0
            ).roundToInt().coerceIn(0, 100)

        if (nowWall - lastMemorySampleMs >= MEMORY_SAMPLE_INTERVAL_MS) {
            activityManager.getMemoryInfo(memoryInfo)
            cachedMemoryUsedMb =
                ((memoryInfo.totalMem - memoryInfo.availMem) / BYTES_PER_MB).toInt()
            cachedMemoryTotalMb =
                (memoryInfo.totalMem / BYTES_PER_MB).toInt()
            lastMemorySampleMs = nowWall
        }

        val thermals = ThermalSensorReader.read(context)
        val heapUsedMb =
            ((runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB).toInt()
        val heapMaxMb = (runtime.maxMemory() / BYTES_PER_MB).toInt()

        return GamePerformanceStats(
            fps = fps,
            frameTimeMs = if (fps > 0) 1000f / fps else 0f,
            systemMemoryUsedMb = cachedMemoryUsedMb,
            systemMemoryTotalMb = cachedMemoryTotalMb,
            processHeapUsedMb = heapUsedMb,
            processHeapMaxMb = heapMaxMb,
            processCpuPercent = cpuPercent,
            gpuRenderer = rendererName,
            gpuLoadPercent = thermals.gpuLoadPercent,
            graphicsApi = graphicsApiName,
            displayRefreshRateHz = readDisplayRefreshRate(),
            cpuTempC = thermals.cpuTempC,
            gpuTempC = thermals.gpuTempC,
            batteryTempC = thermals.batteryTempC,
            batteryPercent = thermals.batteryPercent
        )
    }
}
