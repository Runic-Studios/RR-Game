package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player

/**
 * Draws a horizontal arc of particles around [location]. Supports full 360° or semi-circle (yaw to
 * yaw+180°). Runs asynchronously for performance.
 */
class HorizontalCircleFrame(
    private val radius: Double = 1.5,
    private val semiCircle: Boolean = false,
) : ParticleFormat {

    override fun playParticle(
        player: Player,
        particle: Particle,
        location: Location,
        particleSpacing: Double,
        vararg color: Color,
    ) {
        val startAngle = if (semiCircle) Math.toRadians(location.yaw.toDouble()) else 0.0
        val endAngle = if (semiCircle) startAngle + PI else 2 * PI
        val dustOptions =
            if (particle == Particle.DUST && color.isNotEmpty()) {
                Particle.DustOptions(color[0], 1.0f)
            } else null

        var angle = startAngle
        while (angle < endAngle) {
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            val loc = location.clone().add(x, 0.0, z)
            when {
                dustOptions != null -> {
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
                }
                particle == Particle.BLOCK_CRUMBLE -> {
                    location.world?.spawnParticle(
                        Particle.BLOCK_CRUMBLE,
                        loc,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        Material.COAL_BLOCK.createBlockData(),
                    )
                }
                else -> location.world?.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0)
            }
            angle += particleSpacing
        }
    }
}
