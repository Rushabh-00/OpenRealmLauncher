package dev.openrealm.launcher.utils.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class ThermalStats(
    val cpuTempC: Float? = null,
    val gpuTempC: Float? = null,
    val batteryTempC: Float? = null,
    val batteryPercent: Int? = null,
    val gpuLoadPercent: Int? = null
)

object ThermalSensorReader {
    private data class Sensor(
        val type: String,
        val tempFile: File,
        val cpu: Boolean,
        val gpu: Boolean
    )

    private const val SENSOR_SAMPLE_INTERVAL_MS = 1000L
    private const val BATTERY_SAMPLE_INTERVAL_MS = 2000L

    private var cachedSensors: List<Sensor>? = null
    private var cachedGpuLoadFiles: List<File>? = null
    private var lastBatteryReadMs = -BATTERY_SAMPLE_INTERVAL_MS
    private var cachedBattery: Pair<Float?, Int?> = null to null
    private var lastSampleMs = -SENSOR_SAMPLE_INTERVAL_MS
    private var cachedStats = ThermalStats()

    @Synchronized
    fun read(context: Context): ThermalStats {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSampleMs < SENSOR_SAMPLE_INTERVAL_MS) return cachedStats
        lastSampleMs = now

        val battery = if (now - lastBatteryReadMs >= BATTERY_SAMPLE_INTERVAL_MS) {
            readBattery(context).also {
                cachedBattery = it
                lastBatteryReadMs = now
            }
        } else {
            cachedBattery
        }

        val sensors = sensors()
        val cpu = sensors.firstOrNull(Sensor::cpu)?.let(::readTemperature)
        val gpu = sensors.firstOrNull(Sensor::gpu)?.let(::readTemperature)

        cachedStats = ThermalStats(
            cpuTempC = cpu,
            gpuTempC = gpu,
            batteryTempC = battery.first,
            batteryPercent = battery.second,
            gpuLoadPercent = readGpuLoad()
        )
        return cachedStats
    }

    private fun sensors(): List<Sensor> {
        cachedSensors?.let { return it }
        val root = File("/sys/class/thermal")
        val found = root.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.name.startsWith("thermal_zone") }
            .mapNotNull { zone ->
                val type = runCatching { File(zone, "type").readText().trim() }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                val temp = File(zone, "temp").takeIf { it.isFile }
                if (type != null && temp != null) {
                    Sensor(type, temp, isCpu(type), isGpu(type))
                } else {
                    null
                }
            }
            .toList()
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
        val direct = cachedGpuLoadFiles ?: listOf(
            File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"),
            File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percent"),
            File("/sys/class/kgsl/kgsl-3d0/gpubusy"),
            File("/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load"),
            File("/sys/class/devfreq/gpu/load"),
            File("/sys/class/misc/mali0/device/utilisation"),
            File("/sys/kernel/gpu/gpu_busy")
        ).also { cachedGpuLoadFiles = it }

        direct.forEach { file ->
            parseGpuLoad(file)?.let { value ->
                if (value in 0..100) return value
            }
        }

        val mtk = File("/proc/mtk_mali/utilization")
        if (mtk.isFile && mtk.canRead()) {
            return runCatching {
                BufferedReader(FileReader(mtk)).useLines { lines ->
                    lines.firstNotNullOfOrNull { line ->
                        val marker = "ACTIVE="
                        val start = line.indexOf(marker)
                        if (start < 0) null
                        else line.substring(start + marker.length)
                            .takeWhile(Char::isDigit)
                            .toIntOrNull()
                    }
                }
            }.getOrNull()?.coerceIn(0, 100)
        }

        return null
    }

    private fun parseGpuLoad(file: File): Int? {
        if (!file.isFile || !file.canRead()) return null
        val line = runCatching { file.readText().trim() }.getOrNull().orEmpty()
        if (line.isEmpty()) return null

        line.removeSuffix("%").toIntOrNull()?.let { return it }

        val parts = line.trim().split(' ', '\t').filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            val busy = parts[0].toLongOrNull()
            val total = parts[1].toLongOrNull()
            if (busy != null && total != null && total > 0L) {
                return ((busy * 100L) / total).toInt()
            }
        }

        return line.filter(Char::isDigit).toIntOrNull()
    }
}
