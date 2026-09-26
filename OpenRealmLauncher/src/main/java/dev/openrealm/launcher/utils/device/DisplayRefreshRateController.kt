package dev.openrealm.launcher.utils.device

import android.app.Activity
import android.os.Build
import android.view.Display
import dev.openrealm.launcher.utils.logging.Logger
import kotlin.math.roundToInt

object DisplayRefreshRateController {
    private const val TAG = "DisplayRefreshRate"

    fun getHighestRefreshRate(activity: Activity): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return runCatching {
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display ?: activity.windowManager.defaultDisplay
            } else {
                activity.windowManager.defaultDisplay
            }
            val supportedModes = display.supportedModes
                ?.filter { it.getRefreshRate().isFinite() }
                .orEmpty()
            if (supportedModes.isEmpty()) return@runCatching null
            @Suppress("DEPRECATION")
            val currentMode = display.mode
            val sameResolution = supportedModes.filter { mode ->
                mode.getPhysicalWidth() == currentMode.getPhysicalWidth() &&
                    mode.getPhysicalHeight() == currentMode.getPhysicalHeight()
            }
            (sameResolution.ifEmpty { supportedModes })
                .maxByOrNull { it.getRefreshRate() }
                ?.getRefreshRate()
        }.onFailure {
            Logger.warning(TAG, "Unable to read supported display refresh rates", it)
        }.getOrNull()
    }

    /**
     * Returns refresh-rate choices exposed by Android at the current display resolution.
     */
    fun getSupportedRefreshRates(activity: Activity): List<Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()

        return runCatching {
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display ?: activity.windowManager.defaultDisplay
            } else {
                activity.windowManager.defaultDisplay
            }
            val currentMode = display.mode
            display.supportedModes
                ?.asSequence()
                ?.filter { it.getRefreshRate().isFinite() }
                ?.filter {
                    it.getPhysicalWidth() == currentMode.getPhysicalWidth() &&
                        it.getPhysicalHeight() == currentMode.getPhysicalHeight()
                }
                ?.map { it.getRefreshRate().roundToInt() }
                ?.filter { it > 0 }
                ?.distinct()
                ?.sorted()
                ?.toList()
                .orEmpty()
        }.onFailure {
            Logger.warning(TAG, "Unable to enumerate supported display refresh rates", it)
        }.getOrDefault(emptyList())
    }

    /**
     * Returns the Smart FPS target. 0 selects the highest supported refresh rate;
     * 260 is Minecraft's unlimited FPS option.
     */
    fun getSmartFpsTarget(activity: Activity, preference: Int): Int? {
        if (preference == 260) return 260
        val supported = getSupportedRefreshRates(activity)
        if (supported.isEmpty()) return null
        return if (preference > 0 && preference in supported) preference else supported.maxOrNull()
    }

    fun apply(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display ?: activity.windowManager.defaultDisplay
            } else {
                activity.windowManager.defaultDisplay
            }
            val supportedModes = display.supportedModes
                ?.filter { it.getRefreshRate().isFinite() }
                .orEmpty()
            if (supportedModes.isEmpty()) return
            @Suppress("DEPRECATION")
            val currentMode = display.mode
            val sameResolution = supportedModes.filter {
                it.getPhysicalWidth() == currentMode.getPhysicalWidth() &&
                    it.getPhysicalHeight() == currentMode.getPhysicalHeight()
            }
            val target = (sameResolution.ifEmpty { supportedModes })
                .maxByOrNull({ it.getRefreshRate() })
                ?: return
            @Suppress("DEPRECATION")
            val attributes = activity.window.attributes
            if (attributes.preferredDisplayModeId != target.modeId) {
                attributes.preferredDisplayModeId = target.modeId
                activity.window.attributes = attributes
            }
            Logger.info(TAG, "Display refresh request: " + target.getRefreshRate().roundToInt() + "Hz")
        }.onFailure {
            Logger.warning(TAG, "Unable to request display refresh rate", it)
        }
    }
}