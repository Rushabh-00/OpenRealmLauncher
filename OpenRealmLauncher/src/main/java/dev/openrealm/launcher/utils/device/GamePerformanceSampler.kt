package dev.openrealm.launcher.utils.device

import android.app.ActivityManager
import android.content.Context
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
    }

    private val runtime = Runtime.getRuntime()
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()

    private var lastCpuTimeMs = Process.getElapsedCpuTime()
    private var lastWallTimeMs = SystemClock.elapsedRealtime()

    private val rendererName by lazy {
        runCatching { Renderers.getCurrentRenderer().getRendererName() }.getOrDefault("Unknown")
    }

    private val graphicsApiName by lazy {
        runCatching {
            version.getGraphicsApi().displayName.ifBlank { "Default" }
        }.getOrDefault("Default")
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

        activityManager.getMemoryInfo(memoryInfo)
        val thermals = ThermalSensorReader.read(context)

        // Minecraft and the launcher share the Android process. Runtime heap is
        // therefore a useful low-cost indicator of the Java heap currently available
        // to the game, while system memory remains available for diagnostics.
        val heapUsedMb = ((runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB).toInt()
        val heapMaxMb = (runtime.maxMemory() / BYTES_PER_MB).toInt()

        return GamePerformanceStats(
            fps = fps,
            frameTimeMs = if (fps > 0) 1000f / fps else 0f,
            systemMemoryUsedMb = ((memoryInfo.totalMem - memoryInfo.availMem) / BYTES_PER_MB).toInt(),
            systemMemoryTotalMb = (memoryInfo.totalMem / BYTES_PER_MB).toInt(),
            processHeapUsedMb = heapUsedMb,
            processHeapMaxMb = heapMaxMb,
            processCpuPercent = cpuPercent,
            gpuRenderer = rendererName,
            gpuLoadPercent = thermals.gpuLoadPercent,
            graphicsApi = graphicsApiName,
            cpuTempC = thermals.cpuTempC,
            gpuTempC = thermals.gpuTempC,
            batteryTempC = thermals.batteryTempC,
            batteryPercent = thermals.batteryPercent
        )
    }
}
