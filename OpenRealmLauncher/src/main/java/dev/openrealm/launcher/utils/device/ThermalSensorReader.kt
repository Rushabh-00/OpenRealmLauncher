package dev.openrealm.launcher.utils.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File

data class ThermalStats(
    val cpuTempC: Float? = null,
    val gpuTempC: Float? = null,
    val batteryTempC: Float? = null,
    val batteryPercent: Int? = null,
    val gpuLoadPercent: Int? = null
)

object ThermalSensorReader {
    private data class Sensor(val type: String, val tempFile: File)
    private var cachedSensors: List<Sensor>? = null
    private var lastBatteryReadMs = 0L
    private var cachedBattery: Pair<Float?, Int?> = null to null

    fun read(context: Context): ThermalStats {
        val now = android.os.SystemClock.elapsedRealtime()
        val battery = if (now - lastBatteryReadMs >= BATTERY_SAMPLE_INTERVAL_MS) {
            readBattery(context).also {
                cachedBattery = it
                lastBatteryReadMs = now
            }
        } else {
            cachedBattery
        }
        val sensors = sensors()
        val cpu = sensors.firstOrNull { isCpu(it.type) }?.let(::readTemperature)
        val gpu = sensors.firstOrNull { isGpu(it.type) }?.let(::readTemperature)
        return ThermalStats(
            cpuTempC = cpu,
            gpuTempC = gpu,
            batteryTempC = battery.first,
            batteryPercent = battery.second,
            gpuLoadPercent = readGpuLoad()
        )
    }

    private fun sensors(): List<Sensor> {
        cachedSensors?.let { return it }
        val root = File("/sys/class/thermal")
        val found = root.listFiles()
            .orEmpty()
            .filter { it.name.startsWith("thermal_zone") }
            .mapNotNull { zone ->
                val type = runCatching { File(zone, "type").readText().trim() }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                val temp = File(zone, "temp").takeIf { it.isFile }
                if (type != null && temp != null) Sensor(type, temp) else null
            }
        cachedSensors = found
        return found
    }

    private fun isCpu(type: String): Boolean {
        val t = type.lowercase()
        return t.contains("cpu") || t.contains("soc") || t.contains("apc") ||
            t.contains("tsens") || t.contains("cluster") || t.contains("package")
    }

    private fun isGpu(type: String): Boolean {
        val t = type.lowercase()
        return t.contains("gpu") || t.contains("kgsl") || t.contains("gfx") ||
            t.contains("adreno") || t.contains("mali") || t.contains("panfrost")
    }

    private fun readTemperature(sensor: Sensor): Float? {
        return runCatching {
            val raw = sensor.tempFile.readText().trim().toFloat()
            if (raw > 1000f) raw / 1000f else if (raw > 100f) raw / 10f else raw
        }.getOrNull()?.takeIf { it in -20f..120f }
    }

    private fun readBattery(context: Context): Pair<Float?, Int?> {
        return runCatching {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val temp = rawTemp?.takeIf { it != Int.MIN_VALUE }?.div(10f)
            val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else null
            temp to percent
        }.getOrDefault(null to null)
    }

    private fun readGpuLoad(): Int? {
        val direct = listOf(
            File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"),
            File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percent"),
            File("/sys/class/devfreq/gpu/load")
        )
        for (file in direct) {
            val value = runCatching { file.readText().trim().removeSuffix("%").toInt() }.getOrNull()
            if (value != null && value in 0..100) return value
        }
        return null
    }
    private const val BATTERY_SAMPLE_INTERVAL_MS = 2000L
}
