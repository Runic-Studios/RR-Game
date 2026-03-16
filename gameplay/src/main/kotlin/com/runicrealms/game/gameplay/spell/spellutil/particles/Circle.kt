package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle

/** Spawns 50 evenly-spaced particles in a horizontal ring at [radius] blocks around [center]. */
object Circle {

    fun createParticleCircle(center: Location, particle: Particle, radius: Double) {
        val step = 2 * PI / 50.0
        var angle = 0.0
        repeat(50) {
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            center.world?.spawnParticle(
                particle,
                center.clone().add(x, 0.0, z),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
            angle += step
        }
    }

    fun createParticleCircle(center: Location, color: Color, radius: Double) {
        val dustOptions = Particle.DustOptions(color, 1.0f)
        val step = 2 * PI / 50.0
        var angle = 0.0
        repeat(50) {
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            center.world?.spawnParticle(
                Particle.DUST,
                center.clone().add(x, 0.0, z),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                dustOptions,
            )
            angle += step
        }
    }
}
