package dev.openrealm.launcher.utils.device

import android.content.Context
import android.os.Build
import dev.openrealm.launcher.game.version.installed.Version
import java.util.Locale
import kotlin.math.roundToInt

data class RenderBenchmarkResult(
    val api: String,
    val renderer: String,
    val averageFps: Int,
    val lowFps: Int,
    val samples: Int,
    val minecraftVersion: String = "",
    val deviceKey: String = ""
)

object RenderBenchmarkStore {
    private const val PREFS = "openrealm_render_benchmark"

    private fun deviceKey(): String =
        listOf(Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT).joinToString("|")

    private fun key(version: Version, api: String): String =
        "device:" + deviceKey() +
            ":version:" + version.getVersionName() +
            ":api:" + api.lowercase(Locale.US)

    fun save(context: Context, version: Version, result: RenderBenchmarkResult) {
        val verified = result.copy(
            minecraftVersion = version.getVersionName(),
            deviceKey = deviceKey()
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(version, result.api), encode(verified))
            .putString(
                "latest:device:" + deviceKey() + ":api:" + result.api.lowercase(Locale.US),
                encode(verified)
            )
            .apply()
    }

    private fun encode(result: RenderBenchmarkResult): String =
        listOf(
            result.renderer,
            result.averageFps,
            result.lowFps,
            result.samples,
            result.minecraftVersion,
            result.deviceKey
        ).joinToString("|")

    private fun decode(api: String, raw: String): RenderBenchmarkResult? {
        val p = raw.split("|")
        if (p.size != 6) return null
        return runCatching {
            RenderBenchmarkResult(
                api = api,
                renderer = p[0],
                averageFps = p[1].toInt(),
                lowFps = p[2].toInt(),
                samples = p[3].toInt(),
                minecraftVersion = p[4],
                deviceKey = p[5]
            )
        }.getOrNull()
    }

    fun load(context: Context, version: Version, api: String): RenderBenchmarkResult? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(version, api), null) ?: return null
        return decode(api, raw)
    }

    fun loadLatest(context: Context, api: String): RenderBenchmarkResult? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(
                "latest:device:" + deviceKey() + ":api:" + api.lowercase(Locale.US),
                null
            ) ?: return null
        return decode(api, raw)
    }

    fun recommendation(context: Context, version: Version): String? {
        val vulkan = load(context, version, "Vulkan")
        val opengl = load(context, version, "OpenGL")
        return when {
            vulkan == null || opengl == null -> null
            vulkan.averageFps > opengl.averageFps + 2 -> "Vulkan"
            opengl.averageFps > vulkan.averageFps + 2 -> "OpenGL"
            vulkan.lowFps > opengl.lowFps -> "Vulkan"
            opengl.lowFps > vulkan.lowFps -> "OpenGL"
            else -> "Tie"
        }
    }

    fun score(samples: List<Int>, api: String, renderer: String): RenderBenchmarkResult? {
        val valid = samples.filter { it > 0 }
        if (valid.size < 4) return null
        val average = valid.average().roundToInt()
        val sorted = valid.sorted()
        val lowIndex = ((sorted.size * 0.01).toInt()).coerceAtMost(sorted.lastIndex)
        return RenderBenchmarkResult(api, renderer, average, sorted[lowIndex], valid.size)
    }
}
