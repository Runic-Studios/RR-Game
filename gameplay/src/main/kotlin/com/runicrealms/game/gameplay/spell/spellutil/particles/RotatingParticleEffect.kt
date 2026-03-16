package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player

/**
 * Each call to [show] advances an orbit angle by PI/16 * step and spawns one particle. Supports
 * optional [Color] for REDSTONE/DUST.
 */
class RotatingParticleEffect(private val radius: Double = 1.0, private val step: Double = 1.0) {

    private var t = 0.0

    fun show(player: Player, particle: Particle, location: Location, color: Color? = null) {
        t += PI / 16.0 * step
        val x = radius * cos(t)
        val z = radius * sin(t)
        val loc = location.clone().add(x, 0.0, z)
        if (particle == Particle.DUST && color != null) {
            player.world.spawnParticle(
                Particle.DUST,
                loc,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                Particle.DustOptions(color, 1.0f),
            )
        } else {
            player.world.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }
}
