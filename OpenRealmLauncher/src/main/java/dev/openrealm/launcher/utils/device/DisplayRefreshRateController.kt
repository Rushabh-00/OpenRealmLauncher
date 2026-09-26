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
                ?.filter { it.refreshRate.isFinite() }
                .orEmpty()
            if (supportedModes.isEmpty()) return@runCatching null
            @Suppress("DEPRECATION")
            val currentMode = display.mode
            val sameResolution = supportedModes.filter { mode ->
                mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
            }
            (sameResolution.ifEmpty { supportedModes })
                .maxByOrNull(Display.Mode::refreshRate)
                ?.refreshRate
        }.onFailure {
            Logger.warning(TAG, "Unable to read supported display refresh rates", it)
        }.getOrNull()
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
                ?.filter { it.refreshRate.isFinite() }
                .orEmpty()
            if (supportedModes.isEmpty()) return
            @Suppress("DEPRECATION")
            val currentMode = display.mode
            val sameResolution = supportedModes.filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            val target = (sameResolution.ifEmpty { supportedModes })
                .maxByOrNull(Display.Mode::refreshRate)
                ?: return
            @Suppress("DEPRECATION")
            val attributes = activity.window.attributes
            if (attributes.preferredDisplayModeId != target.modeId) {
                attributes.preferredDisplayModeId = target.modeId
                activity.window.attributes = attributes
            }
            Logger.info(TAG, "Display refresh request: " + target.refreshRate.roundToInt() + "Hz")
        }.onFailure {
            Logger.warning(TAG, "Unable to request display refresh rate", it)
        }
    }
}