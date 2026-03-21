package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player

/** Draws a vertical circle: vector uses cos(theta) for X and sin(theta) for Y. */
class VertCircleFrame(private val radius: Double = 1.0) : ParticleFormat {

    override fun playParticle(
        player: Player,
        particle: Particle,
        location: Location,
        particleSpacing: Double,
        vararg color: Color,
    ) {
        val dustOptions =
            if (particle == Particle.DUST && color.isNotEmpty()) {
                Particle.DustOptions(color[0], 1.0f)
            } else null

        var theta = 0.0
        while (theta < 2 * PI) {
            val x = radius * cos(theta)
            val y = radius * sin(theta)
            val loc = location.clone().add(x, y, 0.0)
            if (dustOptions != null) {
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
            } else {
                location.world?.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0)
            }
            theta += particleSpacing
        }
    }
}
