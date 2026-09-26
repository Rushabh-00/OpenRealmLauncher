package dev.openrealm.launcher.ui.screens.game.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import dev.openrealm.launcher.ui.components.BackgroundCard
import java.util.Locale

data class GamePerformanceStats(
    val fps: Int = 0,
    val frameTimeMs: Float = 0f,
    val systemMemoryUsedMb: Int = 0,
    val systemMemoryTotalMb: Int = 0,
    val processCpuPercent: Int = 0,
    val gpuRenderer: String = "Unknown",
    val gpuLoadPercent: Int? = null,
    val graphicsApi: String = "Default",
    val cpuTempC: Float? = null,
    val gpuTempC: Float? = null,
    val batteryTempC: Float? = null,
    val batteryPercent: Int? = null
)

@Composable
fun PerformanceOverlay(
    stats: GamePerformanceStats,
    showFps: Boolean,
    showFrameTime: Boolean,
    showMemory: Boolean,
    showCpu: Boolean,
    showGpu: Boolean,
    showGpuLoad: Boolean,
    showGraphicsApi: Boolean,
    showCpuTemp: Boolean,
    showGpuTemp: Boolean,
    showBatteryTemp: Boolean,
    showBattery: Boolean,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    val hasContent = showFps || showFrameTime || showMemory || showCpu || showGpu || showGraphicsApi
    if (!hasContent) return

    BackgroundCard(
        modifier = modifier.alpha(opacity),
        influencedByBackground = false,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showFps) Text("FPS  " + stats.fps)
            if (showFrameTime) Text("Frame  " + String.format(Locale.US, "%.1f", stats.frameTimeMs) + " ms")
            if (showMemory) Text("RAM  " + stats.systemMemoryUsedMb + " / " + stats.systemMemoryTotalMb + " MB")
            if (showCpu) Text("CPU  " + stats.processCpuPercent + "% (launcher process)")
            if (showGpu) Text("GPU  " + stats.gpuRenderer)
            if (showGpuLoad) Text("GPU Load  " + (stats.gpuLoadPercent?.toString() ?: "N/A") + "%")
            if (showGraphicsApi) Text("API  " + stats.graphicsApi)
            if (showCpuTemp) Text("CPU Temp  " + (stats.cpuTempC?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A") + " °C")
            if (showGpuTemp) Text("GPU Temp  " + (stats.gpuTempC?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A") + " °C")
            if (showBatteryTemp) Text("Battery Temp  " + (stats.batteryTempC?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A") + " °C")
            if (showBattery) Text("Battery  " + (stats.batteryPercent?.toString() ?: "N/A") + "%")
        }
    }
}
