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
    private var lastCpuTimeMs = Process.getElapsedCpuTime()
    private var lastWallTimeMs = SystemClock.elapsedRealtime()

    fun sample(fps: Int): GamePerformanceStats {
        val nowCpu = Process.getElapsedCpuTime()
        val nowWall = SystemClock.elapsedRealtime()
        val cpuDelta = (nowCpu - lastCpuTimeMs).coerceAtLeast(0L)
        val wallDelta = (nowWall - lastWallTimeMs).coerceAtLeast(1L)
        lastCpuTimeMs = nowCpu
        lastWallTimeMs = nowWall

        val cpuPercent = (
            cpuDelta.toDouble() /
                (wallDelta.toDouble() * Runtime.getRuntime().availableProcessors()) *
                100.0
            ).roundToInt().coerceIn(0, 100)

        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val renderer = runCatching {
            Renderers.getCurrentRenderer().getRendererName()
        }.getOrDefault("Unknown")

        val graphicsApi = runCatching {
            version.getGraphicsApi().displayName.ifBlank { "Default" }
        }.getOrDefault("Default")

        val thermals = ThermalSensorReader.read(context)

        return GamePerformanceStats(
            fps = fps,
            frameTimeMs = if (fps > 0) 1000f / fps else 0f,
            systemMemoryUsedMb = ((memoryInfo.totalMem - memoryInfo.availMem) / (1024L * 1024L)).toInt(),
            systemMemoryTotalMb = (memoryInfo.totalMem / (1024L * 1024L)).toInt(),
            processCpuPercent = cpuPercent,
            gpuRenderer = renderer,
            gpuLoadPercent = thermals.gpuLoadPercent,
            graphicsApi = graphicsApi,
            cpuTempC = thermals.cpuTempC,
            gpuTempC = thermals.gpuTempC,
            batteryTempC = thermals.batteryTempC,
            batteryPercent = thermals.batteryPercent
        )
    }
}
