package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound

/**
 * Each run() draws a hexagon (6 interpolated line segments, 50 points each) at [location]. Plays a
 * sound each tick. Cancels after [duration] runs. Supports REDSTONE (Color), BLOCK_CRACK
 * (Material), or generic.
 */
class Hexagon(
    private val location: Location,
    private val particle: Particle,
    private val radius: Double = 2.0,
    private val duration: Int = 5,
    private val sound: Sound = Sound.BLOCK_NOTE_BLOCK_PLING,
    private val color: Color? = null,
    private val material: Material? = null,
) {

    private var run = 0

    fun start() {
        val plugin = Bukkit.getServer().pluginManager.plugins[0]
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                { task ->
                    if (run >= duration) {
                        task.cancel()
                        return@runTaskTimer
                    }
                    location.world?.playSound(location, sound, 0.5f, 1.0f)
                    drawHexagon()
                    run++
                },
                0L,
                4L,
            )
    }

    private fun drawHexagon() {
        val dustOptions = if (color != null) Particle.DustOptions(color, 1.0f) else null
        val blockData = material?.createBlockData()
        for (vertex in 0 until 6) {
            val startAngle = (vertex * 60.0) * PI / 180.0
            val endAngle = ((vertex + 1) * 60.0) * PI / 180.0
            for (point in 0..50) {
                val t = point / 50.0
                val angle = startAngle + t * (endAngle - startAngle)
                val x = radius * cos(angle)
                val z = radius * sin(angle)
                val loc = location.clone().add(x, 0.0, z)
                when {
                    dustOptions != null ->
                        location.world?.spawnParticle(
                            Particle.DUST,
                            loc,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            dustOptions,
                        )
                    blockData != null ->
                        location.world?.spawnParticle(
                            Particle.BLOCK_CRUMBLE,
                            loc,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            blockData,
                        )
                    else -> location.world?.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0)
                }
            }
        }
    }
}
