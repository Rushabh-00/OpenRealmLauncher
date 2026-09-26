/*
 * Zalith Launcher 2
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

package com.movtery.zalithlauncher.ui.theme.festivals

import android.annotation.SuppressLint
import android.view.MotionEvent

/**
 * 点击彩蛋的输入观察器
 */
object FestivalTapObserver {
    @SuppressLint("StaticFieldLeak")
    private var engine: FestivalEffectsEngine? = null

    /** 效果需要点击交互时由效果层调用 */
    fun attach(target: FestivalEffectsEngine) {
        engine = target
    }

    /** 效果停用或离开组合时解除 */
    fun detach(target: FestivalEffectsEngine) {
        if (engine === target) {
            engine = null
        }
    }

    fun observe(event: MotionEvent) {
        val target = engine ?: return
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            target.burstAt(event.x, event.y)
        }
    }
}
