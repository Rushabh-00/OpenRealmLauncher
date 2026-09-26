package dev.openrealm.launcher.ui.screens.game.elements

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.openrealm.launcher.ui.components.BackgroundCard
import java.util.Locale
import kotlin.math.roundToInt

data class GamePerformanceStats(
    val fps: Int = 0,
    val frameTimeMs: Float = 0f,
    val systemMemoryUsedMb: Int = 0,
    val systemMemoryTotalMb: Int = 0,
    val processHeapUsedMb: Int = 0,
    val processHeapMaxMb: Int = 0,
    val processCpuPercent: Int = 0,
    val gpuRenderer: String = "Unknown",
    val gpuLoadPercent: Int? = null,
    val graphicsApi: String = "Default",
    val displayRefreshRateHz: Int? = null,
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
    showRefreshRate: Boolean,
    showCpuTemp: Boolean,
    showGpuTemp: Boolean,
    showBatteryTemp: Boolean,
    showBattery: Boolean,
    opacity: Float,
    scale: Float,
    locked: Boolean,
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    onPositionSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasContent = showFps || showFrameTime || showMemory || showCpu || showGpu ||
        showGpuLoad || showGraphicsApi || showRefreshRate || showCpuTemp || showGpuTemp || showBatteryTemp || showBattery
    if (!hasContent) return

    var hudModifier = modifier
        .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
        .graphicsLayer {
            val clampedScale = scale.coerceIn(0.5f, 1.5f)
            scaleX = clampedScale
            scaleY = clampedScale
            transformOrigin = TransformOrigin(0f, 0f)
        }
        .alpha(opacity.coerceIn(0f, 1f))

    if (!locked) {
        hudModifier = hudModifier.pointerInput(locked) {
            var dragPosition = position
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragPosition += dragAmount
                    onPositionChanged(dragPosition)
                },
                onDragEnd = onPositionSave
            )
        }
    }

    BackgroundCard(
        modifier = hudModifier,
        influencedByBackground = false,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showFps) Text("FPS  " + stats.fps)
            if (showFrameTime) Text(
                "Frame  " + String.format(Locale.US, "%.1f", stats.frameTimeMs) + " ms"
            )
            if (showMemory) Text(
                "RAM  " + stats.processHeapUsedMb + " / " + stats.processHeapMaxMb + " MB"
            )
            if (showCpu) Text("CPU  " + stats.processCpuPercent + "% (game process)")
            if (showGpu) Text("GPU  " + stats.gpuRenderer)
            if (showGpuLoad) Text(
                "GPU Load  " + (stats.gpuLoadPercent?.toString() ?: "N/A") + "%"
            )
            if (showGraphicsApi) Text("Renderer  " + stats.graphicsApi)
            if (showRefreshRate) Text("Display  " + (stats.displayRefreshRateHz?.let { "$it Hz" } ?: "N/A"))
            if (showCpuTemp) Text(
                "CPU Temp  " +
                    (stats.cpuTempC?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A") +
                    " °C"
            )
            if (showGpuTemp) Text(
                "GPU Temp  " +
                    (stats.gpuTempC?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A") +
                    " °C"
            )
            if (showBatteryTemp) Text(
                "Battery Temp  " +
                    (stats.batteryTempC?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A") +
                    " °C"
            )
            if (showBattery) Text(
                "Battery  " + (stats.batteryPercent?.toString() ?: "N/A") + "%"
            )
        }
    }
}
