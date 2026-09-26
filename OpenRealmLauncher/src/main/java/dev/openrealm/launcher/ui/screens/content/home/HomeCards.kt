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

package dev.openrealm.launcher.ui.screens.content.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.cardgrid.model.CardLimits
import com.movtery.cardgrid.model.CardType
import dev.openrealm.launcher.BuildConfig
import dev.openrealm.launcher.BuildKeys
import dev.openrealm.launcher.R
import dev.openrealm.launcher.ui.components.BackgroundCard
import dev.openrealm.launcher.ui.screens.content.home.version.VersionCardContent

/** 系统卡片（不可变更），由启动器自行提供并绘制在网格之外 */
class SystemCard(val id: String, val content: @Composable () -> Unit)

data class HomeQuickActionActions(
    val lastPlayed: () -> Unit,
    val instances: () -> Unit,
    val servers: () -> Unit,
    val downloads: () -> Unit,
    val mods: () -> Unit,
)

val LocalHomeQuickActionActions = staticCompositionLocalOf<HomeQuickActionActions?> { null }

private data class HomeQuickAction(
    val id: String,
    val titleRes: Int,
    val action: HomeQuickActionActions.() -> Unit
)

private val homeQuickActions = listOf(
    HomeQuickAction("last_played", R.string.home_quick_last_played) { lastPlayed() },
    HomeQuickAction("instances", R.string.home_quick_instances) { instances() },
    HomeQuickAction("servers", R.string.home_quick_servers) { servers() },
    HomeQuickAction("downloads", R.string.home_quick_downloads) { downloads() },
    HomeQuickAction("mods", R.string.home_quick_mods) { mods() },
)


/**
 * 主页卡片注册表
 */
object HomeCards {
    /** 版本卡片的类型 id */
    const val VERSION_CARD_TYPE_ID = "version_card"

    private val versionCardType = CardType(
        typeId = VERSION_CARD_TYPE_ID,
        defaultSpan = IntOffset(10, 6),
        limits = CardLimits(
            minWidth = 10,
            minHeight = 4,
            maxHeight = 12
        ),
        content = { cardId ->
            VersionCardContent(cardId)
        }
    )

    /** 版本卡片类型 */
    fun versionCardType(): CardType = versionCardType

    /** 用户卡片类型注册表 */
    val userCardTypes: List<CardType> = listOf(versionCardType)

    /** 系统卡片（不可变更） */
    fun systemCards(actions: HomeQuickActionActions? = null): List<SystemCard> = buildList {
        actions?.let { actionSet ->
            val enabled = AllSettings.homeQuickActions.state
            val selected = homeQuickActions.filter { it.id in enabled }
            if (selected.isNotEmpty()) {
                add(
                    SystemCard(id = "system_home_quick_actions") {
                        BackgroundCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.home_quick_actions_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                ) {
                                    selected.forEach { item ->
                                        Surface(
                                            modifier = Modifier.clickable { item.action(actionSet) },
                                            shape = MaterialTheme.shapes.large,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Text(
                                                text = stringResource(item.titleRes),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        if (BuildConfig.DEBUG) {
            add(debugWarningCard())
        }
    }

    /**
     * debug版本关不掉的警告，防止有人把测试版当正式版用 XD
     */
    private fun debugWarningCard() = SystemCard(id = "system_debug_warning") {
        BackgroundCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.generic_warning),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.launcher_version_debug_warning, BuildKeys.LAUNCHER_NAME),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    modifier = Modifier
                        .alpha(0.8f)
                        .align(Alignment.End),
                    text = stringResource(R.string.launcher_version_debug_warning_cant_close),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
