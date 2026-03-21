package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player

/**
 * Like [HelixParticleFrame] but the radius expands from 0 to [endRadius] as height increases.
 * `stageRadius = (endRadius / height) * (degreesElapsed / totalDegrees)`
 */
class HelixReverseParticleFrame(
    private val height: Double = 2.0,
    private val endRadius: Double = 1.0,
    private val frequency: Double = 1.0,
) : ParticleFormat {

    override fun playParticle(
        player: Player,
        particle: Particle,
        location: Location,
        particleSpacing: Double,
        vararg color: Color,
    ) {
        val totalDegrees = (360.0 / frequency) * height
        var degrees = 0.0
        while (degrees < totalDegrees) {
            val stageRadius = (endRadius / totalDegrees) * degrees
            val rad = Math.toRadians(degrees)
            val x = stageRadius * cos(rad)
            val z = stageRadius * sin(rad)
            val y = (degrees / totalDegrees) * height
            val loc = location.clone().add(x, y, z)

            if (particle == Particle.DUST && color.isNotEmpty()) {
                player.world.spawnParticle(
                    Particle.DUST,
                    loc,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    Particle.DustOptions(color[0], 1.0f),
                )
            } else {
                player.world.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0)
            }
            degrees += particleSpacing * 10.0
        }
    }
}
