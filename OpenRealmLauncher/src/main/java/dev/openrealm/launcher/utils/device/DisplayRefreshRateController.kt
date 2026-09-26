package dev.openrealm.launcher.utils.device

import android.app.Activity
import android.os.Build
import dev.openrealm.launcher.utils.logging.Logger

object DisplayRefreshRateController {
    private const val TAG = "DisplayRefreshRate"

    fun apply(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display ?: activity.windowManager.defaultDisplay
            } else {
                activity.windowManager.defaultDisplay
            }
            val target = display.supportedModes
                ?.filter { it.refreshRate.isFinite() }
                ?.maxByOrNull { it.refreshRate }
                ?: return
            @Suppress("DEPRECATION")
            val attrs = activity.window.attributes
            if (attrs.preferredDisplayModeId == target.modeId) return
            attrs.preferredDisplayModeId = target.modeId
            activity.window.attributes = attrs
            Logger.info(TAG, "Display refresh request: " + target.refreshRate + "Hz")
        }.onFailure {
            Logger.warning(TAG, "Unable to request display refresh rate", it)
        }
    }
}
