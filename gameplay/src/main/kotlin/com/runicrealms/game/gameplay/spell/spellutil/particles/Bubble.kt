package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle

/**
 * Animates a sphere oscillating from top to bottom using spherical coords (theta/phi loop). Cancels
 * after [piCoefficient] * PI radians of phi rotation.
 */
object Bubble {

    fun bubbleEffect(
        center: Location,
        particle: Particle,
        radius: Double,
        piCoefficient: Double = 2.0,
    ) {
        val plugin = Bukkit.getServer().pluginManager.plugins[0]
        val maxPhi = piCoefficient * PI
        var phi = 0.0

        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                { task ->
                    if (phi >= maxPhi) {
                        task.cancel()
                        return@runTaskTimer
                    }
                    val theta = 0.0
                    var angle = 0.0
                    while (angle < 2 * PI) {
                        val x = radius * sin(phi) * cos(angle)
                        val y = radius * cos(phi)
                        val z = radius * sin(phi) * sin(angle)
                        center.world?.spawnParticle(
                            particle,
                            center.clone().add(x, y, z),
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                        )
                        angle += 0.3
                    }
                    phi += 0.1
                },
                0L,
                1L,
            )
    }
}
