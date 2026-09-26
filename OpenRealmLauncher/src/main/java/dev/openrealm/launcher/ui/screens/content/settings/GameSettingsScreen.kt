/*
 * OpenRealm Launcher
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package dev.openrealm.launcher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.openrealm.launcher.R
import dev.openrealm.launcher.game.multirt.RuntimesManager
import dev.openrealm.launcher.game.plugin.natives.NativePlugin
import dev.openrealm.launcher.game.plugin.natives.NativePluginManager
import dev.openrealm.launcher.path.URL_CLOUD_NATIVE_LIB_PLUGINS
import dev.openrealm.launcher.path.URL_GITHUB_NATIVE_LIB_PLUGINS
import dev.openrealm.launcher.setting.AllSettings
import dev.openrealm.launcher.setting.unit.floatRange
import dev.openrealm.launcher.setting.unit.min
import dev.openrealm.launcher.ui.base.BaseScreen
import dev.openrealm.launcher.ui.components.AnimatedColumn
import dev.openrealm.launcher.ui.components.verticalScrollWithBar
import dev.openrealm.launcher.ui.screens.NestedNavKey
import dev.openrealm.launcher.ui.screens.NormalNavKey
import dev.openrealm.launcher.ui.screens.TitledNavKey
import dev.openrealm.launcher.ui.screens.content.elements.MemoryPreview
import dev.openrealm.launcher.ui.screens.content.settings.layouts.CardPosition
import dev.openrealm.launcher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import dev.openrealm.launcher.ui.screens.content.settings.layouts.ListSettingsCard
import dev.openrealm.launcher.ui.screens.content.settings.layouts.SettingsCard
import dev.openrealm.launcher.ui.screens.content.settings.layouts.SettingsCardColumn
import dev.openrealm.launcher.ui.screens.content.settings.layouts.StringListSettingsCard
import dev.openrealm.launcher.ui.screens.content.settings.layouts.SwitchSettingsCard
import dev.openrealm.launcher.ui.screens.content.settings.layouts.TextInputSettingsCard
import dev.openrealm.launcher.utils.device.DisplayRefreshRateController
import dev.openrealm.launcher.utils.platform.getMaxMemoryForSettings
import dev.openrealm.launcher.utils.platform.getRecommendedMemoryForMinecraft
import dev.openrealm.launcher.viewmodel.EventViewModel
import dev.openrealm.launcher.viewmodel.sendDLPlugin

@Composable
fun GameSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Game, settingsScreenKey, false)
    ) { isVisible ->
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) { scope ->
            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.versionIsolation,
                        title = stringResource(R.string.settings_game_version_isolation_title),
                        summary = stringResource(R.string.settings_game_version_isolation_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.skipGameIntegrityCheck,
                        title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
                        summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
                    )

                    TextInputSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.versionCustomInfo,
                        title = stringResource(R.string.settings_game_version_custom_info_title),
                        summary = stringResource(R.string.settings_game_version_custom_info_summary)
                    )
                }
            }

            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    val runtimes = remember { RuntimesManager.getRuntimes() }

                    if (runtimes.isNotEmpty()) {
                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top,
                            unit = AllSettings.javaRuntime,
                            items = RuntimesManager.getRuntimes().filter { it.isCompatible() },
                            title = stringResource(R.string.settings_game_java_runtime_title),
                            summary = stringResource(R.string.settings_game_java_runtime_summary),
                            getItemText = { it.name },
                            getItemId = { it.name }
                        )
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = if (runtimes.isNotEmpty()) CardPosition.Middle else CardPosition.Top,
                        unit = AllSettings.autoPickJavaRuntime,
                        title = stringResource(R.string.settings_game_auto_pick_java_runtime_title),
                        summary = stringResource(R.string.settings_game_auto_pick_java_runtime_summary)
                    )

                    val nativePlugins = remember {
                        NativePluginManager.getPlugins()
                    }

                    @Composable
                    fun DLNativeLibsButton() {
                        IconButton(
                            onClick = {
                                eventViewModel.sendDLPlugin(
                                    githubLink = URL_GITHUB_NATIVE_LIB_PLUGINS,
                                    cloudDrives = listOf(
                                        EventViewModel.Event.DownloadPlugins.CloudDrive(
                                            language = "zh",
                                            link = URL_CLOUD_NATIVE_LIB_PLUGINS
                                        )
                                    )
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download_2_filled),
                                contentDescription = stringResource(R.string.generic_download)
                            )
                        }
                    }
                    if (nativePlugins.isNotEmpty()) {
                        StringListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.disableNativeLibPlugins,
                            items = nativePlugins,
                            onItemsChange = { value, item ->
                                if (value) {
                                    this - item.packageName
                                } else {
                                    this + item.packageName
                                }
                            },
                            title = stringResource(R.string.settings_game_native_lib_plugin_title),
                            summary = stringResource(R.string.settings_game_native_lib_plugin_summary),
                            getItemID = { it.packageName },
                            getItemText = { it.displayName },
                            getItemSummary = { plugin ->
                                NativePluginSummaryLayout(plugin)
                            },
                            getItemCheck = { contains -> !contains },
                            trailingIcon = {
                                DLNativeLibsButton()
                            },
                        )
                    } else {
                        SettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            title = stringResource(R.string.settings_game_native_lib_plugin_title),
                            summary = stringResource(R.string.settings_game_native_lib_plugin_summary),
                            onClick = {},
                            trailingIcon = {
                                DLNativeLibsButton()
                            }
                        )
                    }

                    val gameSettingsContext = LocalContext.current
                    val gameActivity = gameSettingsContext as? Activity
                    val supportedRefreshRates = remember(gameActivity) {
                        gameActivity?.let { DisplayRefreshRateController.getSupportedRefreshRates(it) }.orEmpty()
                    }
                    val smartFpsChoices = remember(supportedRefreshRates) {
                        buildList {
                            add(0)
                            addAll(supportedRefreshRates)
                            add(260)
                        }.distinct()
                    }
                    val detectedRefresh = supportedRefreshRates.maxOrNull()
                    val recommendedMemory = remember(gameSettingsContext) {
                        getRecommendedMemoryForMinecraft(gameSettingsContext)
                    }
                    val maximumSafeMemory = remember(gameSettingsContext) {
                        getMaxMemoryForSettings(gameSettingsContext)
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.autoRamAllocation,
                        title = stringResource(R.string.settings_game_auto_ram_title),
                        summary = stringResource(
                            R.string.settings_game_auto_ram_summary,
                            recommendedMemory
                        )
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.smartFpsMode,
                        title = stringResource(R.string.settings_game_smart_fps_title),
                        summary = stringResource(
                            R.string.settings_game_smart_fps_summary,
                            detectedRefresh ?: 0
                        )
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        items = smartFpsChoices.map(Int::toString),
                        currentId = AllSettings.smartFpsLimit.state.toString(),
                        defaultId = "0",
                        title = stringResource(R.string.settings_game_smart_fps_limit_title),
                        summary = stringResource(R.string.settings_game_smart_fps_limit_summary),
                        getItemId = { it },
                        getItemText = { value ->
                            when (value.toInt()) {
                                0 -> stringResource(R.string.settings_game_smart_fps_auto)
                                260 -> stringResource(R.string.settings_game_smart_fps_unlimited)
                                else -> value + " FPS"
                            }
                        },
                        enabled = AllSettings.smartFpsMode.state,
                        onValueChange = { AllSettings.smartFpsLimit.save(it.toInt()) }
                    )
                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.ramAllocation,
                        title = stringResource(R.string.settings_game_java_memory_title),
                        summary = stringResource(R.string.settings_game_java_memory_summary),
                        valueRange = AllSettings.ramAllocation.floatRange.start..maximumSafeMemory.toFloat(),
                        enabled = !AllSettings.autoRamAllocation.state,
                        suffix = "MB",
                        fineTuningControl = true,
                        previewContent = {
                            MemoryPreview(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 2.dp),
                                preview = (AllSettings.ramAllocation.state ?: AllSettings.ramAllocation.min).toDouble(),
                                usedText = { usedMemory, totalMemory ->
                                    stringResource(R.string.settings_game_java_memory_used_text, usedMemory.toInt(), totalMemory.toInt())
                                },
                                previewText = { preview ->
                                    stringResource(R.string.settings_game_java_memory_allocation_text, preview.toInt())
                                }
                            )
                        }
                    )

                    TextInputSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.jvmArgs,
                        title = stringResource(R.string.settings_game_jvm_args_title),
                        summary = stringResource(R.string.settings_game_jvm_args_summary)
                    )
                }
            }

            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.performanceOverlayEnabled,
                        title = stringResource(R.string.settings_game_performance_overlay_title),
                        summary = stringResource(R.string.settings_game_performance_overlay_summary)
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayOpacity,
                        title = stringResource(R.string.settings_game_performance_overlay_opacity_title),
                        summary = stringResource(R.string.settings_game_performance_overlay_opacity_summary),
                        valueRange = AllSettings.performanceOverlayOpacity.floatRange,
                        suffix = "%"
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayScale,
                        title = stringResource(R.string.settings_game_performance_overlay_scale_title),
                        summary = stringResource(R.string.settings_game_performance_overlay_scale_summary),
                        valueRange = AllSettings.performanceOverlayScale.floatRange,
                        suffix = "%",
                        fineTuningControl = true
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayLocked,
                        title = stringResource(R.string.settings_game_performance_overlay_lock_title),
                        summary = stringResource(R.string.settings_game_performance_overlay_lock_summary)
                    )

                    SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        title = stringResource(R.string.settings_game_performance_overlay_reset_position_title),
                        summary = stringResource(R.string.settings_game_performance_overlay_reset_position_summary),
                        onClick = {
                            AllSettings.performanceOverlayPosition.updateState(Offset.Zero)
                            AllSettings.performanceOverlayPosition.save()
                        }
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowFps,
                        title = stringResource(R.string.settings_game_performance_fps_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowFrameTime,
                        title = stringResource(R.string.settings_game_performance_frame_time_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowMemory,
                        title = stringResource(R.string.settings_game_performance_memory_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowCpu,
                        title = stringResource(R.string.settings_game_performance_cpu_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowGpu,
                        title = stringResource(R.string.settings_game_performance_gpu_title),
                        summary = stringResource(R.string.settings_game_performance_gpu_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowGpuLoad,
                        title = stringResource(R.string.settings_game_performance_gpu_load_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowCpuTemp,
                        title = stringResource(R.string.settings_game_performance_cpu_temp_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowGpuTemp,
                        title = stringResource(R.string.settings_game_performance_gpu_temp_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowBatteryTemp,
                        title = stringResource(R.string.settings_game_performance_battery_temp_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.performanceOverlayShowBattery,
                        title = stringResource(R.string.settings_game_performance_battery_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.performanceOverlayShowGraphicsApi,
                        title = stringResource(R.string.settings_game_performance_api_title),
                        summary = stringResource(R.string.settings_game_performance_metric_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.showLogAutomatic,
                        title = stringResource(R.string.settings_game_show_log_automatic_title),
                        summary = stringResource(R.string.settings_game_show_log_automatic_summary)
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.logTextSize,
                        title = stringResource(R.string.settings_game_log_text_size_title),
                        summary = stringResource(R.string.settings_game_log_text_size_summary),
                        valueRange = AllSettings.logTextSize.floatRange,
                        suffix = "Sp",
                        fineTuningControl = true
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.logBufferFlushInterval,
                        title = stringResource(R.string.settings_game_log_buffer_flush_interval_title),
                        summary = stringResource(R.string.settings_game_log_buffer_flush_interval_summary),
                        valueRange = AllSettings.logBufferFlushInterval.floatRange,
                        suffix = "ms",
                        fineTuningControl = true
                    )
                }
            }
        }
    }
}


@Composable
private fun NativePluginSummaryLayout(plugin: NativePlugin) {
    FlowRow(
        modifier = Modifier.alpha(0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_renderer_from_plugins,plugin.appName),
            style = MaterialTheme.typography.labelSmall
        )

        val minVer = plugin.minMCVer
        val maxVer = plugin.maxMCVer

        if (minVer != null || maxVer != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = stringResource(R.string.renderer_version_support), style = MaterialTheme.typography.labelSmall)

                minVer?.let {
                    Text(text = ">= $it", style = MaterialTheme.typography.labelSmall)
                }

                maxVer?.let {
                    Text(text = "<= $it", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}