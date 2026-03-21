package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player

/**
 * Draws a fixed-radius helix spiraling upward over [height] blocks. `totalDegrees = (360 /
 * frequency) * height`
 *
 * Supports: REDSTONE (Color), BLOCK_CRACK (PACKED_ICE), or generic particle.
 */
class HelixParticleFrame(
    private val height: Double = 2.0,
    private val radius: Double = 0.5,
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
            val rad = Math.toRadians(degrees)
            val x = radius * cos(rad)
            val z = radius * sin(rad)
            val y = (degrees / totalDegrees) * height
            val loc = location.clone().add(x, y, z)

            when {
                particle == Particle.DUST && color.isNotEmpty() -> {
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
                }
                particle == Particle.BLOCK_CRUMBLE -> {
                    player.world.spawnParticle(
                        Particle.BLOCK_CRUMBLE,
                        loc,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        Material.PACKED_ICE.createBlockData(),
                    )
                }
                else -> player.world.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0)
            }
            degrees += particleSpacing * 10.0
        }
    }
}
