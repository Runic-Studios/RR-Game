package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle

/**
 * Spawns [particleCount] particles randomly distributed on a sphere of [radius] around [center].
 */
object ParticleSphere {

    fun show(center: Location, particle: Particle, radius: Double, particleCount: Int) {
        repeat(particleCount) {
            val theta = acos(2.0 * Random.nextDouble() - 1.0)
            val phi = 2.0 * PI * Random.nextDouble()
            val x = radius * sin(theta) * cos(phi)
            val y = radius * cos(theta)
            val z = radius * sin(theta) * sin(phi)
            center.world?.spawnParticle(
                particle,
                center.clone().add(x, y, z),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
        }
    }

    fun show(center: Location, color: Color, radius: Double, particleCount: Int) {
        val dustOptions = Particle.DustOptions(color, 1.0f)
        repeat(particleCount) {
            val theta = acos(2.0 * Random.nextDouble() - 1.0)
            val phi = 2.0 * PI * Random.nextDouble()
            val x = radius * sin(theta) * cos(phi)
            val y = radius * cos(theta)
            val z = radius * sin(theta) * sin(phi)
            center.world?.spawnParticle(
                Particle.DUST,
                center.clone().add(x, y, z),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                dustOptions,
            )
        }
    }
}
