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
    val graphicsApi: String = "Default"
)

@Composable
fun PerformanceOverlay(
    stats: GamePerformanceStats,
    showFps: Boolean,
    showFrameTime: Boolean,
    showMemory: Boolean,
    showCpu: Boolean,
    showGpu: Boolean,
    showGraphicsApi: Boolean,
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
            if (showGraphicsApi) Text("API  " + stats.graphicsApi)
        }
    }
}
