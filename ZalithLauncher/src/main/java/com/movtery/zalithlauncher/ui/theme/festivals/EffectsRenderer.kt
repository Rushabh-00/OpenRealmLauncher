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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.movtery.zalithlauncher.ui.theme.festivals.simulations.FirefliesSimulator
import com.movtery.zalithlauncher.ui.theme.festivals.simulations.FireworksSimulator
import com.movtery.zalithlauncher.ui.theme.festivals.simulations.Particle
import com.movtery.zalithlauncher.ui.theme.festivals.simulations.ParticleSimulator
import com.movtery.zalithlauncher.ui.theme.festivals.simulations.RainSimulator
import com.movtery.zalithlauncher.ui.theme.festivals.simulations.SnowSimulator
import kotlin.math.hypot
import kotlin.random.Random

/** 效果绘制器，把对应模拟器的粒子画到画布上 */
interface EffectDrawer {
    /** 深浅主题切换时更新绘制颜色 */
    fun setTheme(isDark: Boolean)
    fun draw(canvas: Canvas, paint: Paint)
}

/** 根据效果种类构建对应的模拟器与绘制器 */
fun createEffect(
    type: FestivalEffectType,
    isDark: Boolean,
    width: Float,
    height: Float,
    density: Float
): Pair<ParticleSimulator, EffectDrawer> {
    val random = Random(System.nanoTime())
    fun buildSimulator(): ParticleSimulator = when (type) {
        FestivalEffectType.RAIN -> RainSimulator(random)
        FestivalEffectType.SNOW -> SnowSimulator(random)
        FestivalEffectType.FIREWORKS -> FireworksSimulator(random)
        FestivalEffectType.FIREWORKS_NATIONAL -> FireworksSimulator(random, grand = true)
        FestivalEffectType.FIREFLIES -> FirefliesSimulator(random)
    }
    val simulator = buildSimulator().also { it.resize(width, height, density) }
    val drawer = when (type) {
        FestivalEffectType.RAIN -> RainDrawer(simulator as RainSimulator)
        FestivalEffectType.SNOW -> SnowDrawer(simulator as SnowSimulator)
        FestivalEffectType.FIREWORKS -> FireworksDrawer(simulator as FireworksSimulator, national = false)
        FestivalEffectType.FIREWORKS_NATIONAL -> FireworksDrawer(simulator as FireworksSimulator, national = true)
        FestivalEffectType.FIREFLIES -> FirefliesDrawer(simulator as FirefliesSimulator)
    }
    drawer.setTheme(isDark)
    return simulator to drawer
}

private fun applyAlpha(paint: Paint, color: Int, particleAlpha: Float) {
    paint.color = color
    paint.alpha = (particleAlpha * Color.alpha(color)).toInt().coerceIn(0, 255)
}

/** 生成一张边缘柔和的径向渐变圆点图 */
private fun softDotBitmap(size: Int, color: Int, coreStop: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val shader = RadialGradient(
        center, center, center,
        intArrayOf(color, color, color and 0x00FFFFFF),
        floatArrayOf(0f, coreStop, 1f),
        Shader.TileMode.CLAMP
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
    canvas.drawCircle(center, center, center, paint)
    return bitmap
}

/** 雨丝与溅花 */
private class RainDrawer(
    private val simulator: RainSimulator
) : EffectDrawer {
    private var streakColor = 0
    private var splashColor = 0

    override fun setTheme(isDark: Boolean) {
        val palette = FestivalPalette.rain(isDark)
        streakColor = palette.streak
        splashColor = palette.splash
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.shader = null
        paint.xfermode = null

        // 屏外粒子只参与模拟，不浪费光栅化
        val width = simulator.width
        val height = simulator.height
        for (particle in simulator.particles) {
            if (particle.x < -CULL_MARGIN || particle.x > width + CULL_MARGIN ||
                particle.y < -CULL_MARGIN - particle.size || particle.y > height + CULL_MARGIN
            ) {
                continue
            }

            if (particle.state == RainSimulator.STATE_DROP) {
                applyAlpha(paint, streakColor, particle.alpha)
                val speed = hypot(particle.vx, particle.vy).coerceAtLeast(1f)
                paint.strokeWidth = (0.5f + particle.variant * 0.7f) * simulator.density
                canvas.drawLine(
                    particle.x, particle.y,
                    particle.x - particle.vx / speed * particle.size,
                    particle.y - particle.vy / speed * particle.size,
                    paint
                )
            } else {
                applyAlpha(paint, splashColor, particle.alpha)
                paint.strokeWidth = particle.size * 2f
                canvas.drawPoint(particle.x, particle.y, paint)
            }
        }
    }

    companion object {
        private const val CULL_MARGIN = 24f
    }
}

/** 雪花 */
private class SnowDrawer(
    private val simulator: SnowSimulator
) : EffectDrawer {
    private var flakeBitmap: Bitmap? = null
    private var flakeColor = 0xFFFFFFFF.toInt()
    private val matrix = Matrix()

    override fun setTheme(isDark: Boolean) {
        flakeColor = FestivalPalette.snow(isDark).flake
        flakeBitmap?.recycle()
        flakeBitmap = softDotBitmap(64, flakeColor, coreStop = 0.42f)
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        val bitmap = flakeBitmap ?: return
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.xfermode = null

        val width = simulator.width
        val height = simulator.height
        for (particle in simulator.particles) {
            if (particle.x < -CULL_MARGIN || particle.x > width + CULL_MARGIN ||
                particle.y < -CULL_MARGIN || particle.y > height + CULL_MARGIN
            ) {
                continue
            }
            applyAlpha(paint, flakeColor, particle.alpha)
            val diameter = particle.size * 2f
            matrix.setScale(diameter / bitmap.width, diameter / bitmap.height)
            matrix.postTranslate(particle.x - particle.size, particle.y - particle.size)
            canvas.drawBitmap(bitmap, matrix, paint)
        }
    }

    companion object {
        private const val CULL_MARGIN = 30f
    }
}

/** 烟花 */
private class FireworksDrawer(
    private val simulator: FireworksSimulator,
    /** 国庆限定红金配色 */
    private val national: Boolean
) : EffectDrawer {
    private val addMode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    private var bursts: List<Int> = emptyList()
    private var flashColor = 0
    private var rocketColor = 0xFFFFFFFF.toInt()
    private var additive = false

    override fun setTheme(isDark: Boolean) {
        val palette = if (national) FestivalPalette.fireworkNational(isDark) else FestivalPalette.firework(isDark)
        bursts = palette.bursts
        flashColor = palette.flash
        rocketColor = palette.rocket
        additive = palette.additive
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.shader = null
        paint.xfermode = if (additive) addMode else null

        val width = simulator.width
        val height = simulator.height
        for (particle in simulator.particles) {
            when (particle.state) {
                FireworksSimulator.STATE_ROCKET -> {
                    if (isOffscreen(particle, width, height)) continue
                    applyAlpha(paint, rocketColor, 1f)
                    paint.strokeWidth = particle.size
                    canvas.drawLine(particle.px, particle.py, particle.x, particle.y, paint)
                }

                FireworksSimulator.STATE_BURST, FireworksSimulator.STATE_SPLIT -> {
                    if (particle.alpha <= NEAR_INVISIBLE_ALPHA || isOffscreen(particle, width, height)) continue
                    applyAlpha(
                        paint,
                        bursts.getOrElse(particle.colorIndex) { 0xFFFFFFFF.toInt() },
                        particle.alpha
                    )
                    paint.strokeWidth = particle.size
                    canvas.drawLine(particle.px, particle.py, particle.x, particle.y, paint)
                }

                FireworksSimulator.STATE_FLASH -> {
                    val progress = particle.life / particle.maxLife
                    paint.style = Paint.Style.FILL
                    applyAlpha(paint, flashColor, 1f - progress)
                    canvas.drawCircle(particle.x, particle.y, particle.size * (1.6f - progress), paint)
                    paint.style = Paint.Style.STROKE
                }
            }
        }
        paint.xfermode = null
    }

    private fun isOffscreen(particle: Particle, width: Float, height: Float): Boolean {
        return particle.x < -CULL_MARGIN || particle.x > width + CULL_MARGIN ||
            particle.y < -CULL_MARGIN || particle.y > height + CULL_MARGIN
    }

    companion object {
        private const val CULL_MARGIN = 60f
        private const val NEAR_INVISIBLE_ALPHA = 0.02f
    }
}

/** 流萤与满月 */
private class FirefliesDrawer(
    private val simulator: FirefliesSimulator
) : EffectDrawer {
    private var coreBitmap: Bitmap? = null
    private var haloBitmap: Bitmap? = null
    private var moonBitmap: Bitmap? = null
    private val matrix = Matrix()

    override fun setTheme(isDark: Boolean) {
        val palette = FestivalPalette.fireflies(isDark)
        coreBitmap?.recycle()
        haloBitmap?.recycle()
        moonBitmap?.recycle()
        coreBitmap = softDotBitmap(32, palette.core, coreStop = 0.5f)
        haloBitmap = softDotBitmap(96, palette.halo, coreStop = 0.12f)
        moonBitmap = moonBitmap(palette)
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        val core = coreBitmap ?: return
        val halo = haloBitmap ?: return
        val moon = moonBitmap ?: return
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.xfermode = null

        // 满月：呼吸光晕 + 月盘
        val moonScale = simulator.moonRadius / (MOON_DISC_STOP * moon.width / 2f)
        val moonSize = moon.width * moonScale
        paint.alpha = (simulator.moonGlow * 255f).toInt().coerceIn(0, 255)
        matrix.reset()
        matrix.setScale(moonScale, moonScale)
        matrix.postTranslate(simulator.moonX - moonSize / 2f, simulator.moonY - moonSize / 2f)
        canvas.drawBitmap(moon, matrix, paint)

        // 流萤：先晕后芯
        for (fly in simulator.flies) {
            paint.alpha = (fly.alpha * 255f).toInt().coerceIn(0, 255)

            val haloSize = simulator.density * HALO_DIAMETER_DP
            matrix.reset()
            matrix.setScale(haloSize / halo.width, haloSize / halo.height)
            matrix.postTranslate(fly.x - haloSize / 2f, fly.y - haloSize / 2f)
            canvas.drawBitmap(halo, matrix, paint)

            val coreSize = simulator.density * CORE_DIAMETER_DP
            matrix.reset()
            matrix.setScale(coreSize / core.width, coreSize / core.height)
            matrix.postTranslate(fly.x - coreSize / 2f, fly.y - coreSize / 2f)
            canvas.drawBitmap(core, matrix, paint)
        }
    }

    /** 月盘实心、外围一圈柔光的径向渐变图 */
    private fun moonBitmap(palette: FestivalPalette.Fireflies): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val transparent = palette.moonHalo and 0x00FFFFFF
        val shader = RadialGradient(
            center, center, center,
            intArrayOf(palette.moon, palette.moon, palette.moonHalo, transparent),
            floatArrayOf(0f, MOON_DISC_STOP, MOON_DISC_STOP + 0.24f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawCircle(center, center, center, paint)
        return bitmap
    }

    companion object {
        private const val CORE_DIAMETER_DP = 2.6f
        private const val HALO_DIAMETER_DP = 8.5f
        private const val MOON_DISC_STOP = 0.18f
    }
}
