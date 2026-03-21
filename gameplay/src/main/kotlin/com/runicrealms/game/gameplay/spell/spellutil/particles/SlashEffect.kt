package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player

/**
 * Slash particle animation utilities. Uses Bukkit scheduler to animate particle sequences over
 * time.
 */
object SlashEffect {

    fun slashVertical(
        player: Player,
        particle: Particle,
        location: Location,
        color: Color? = null,
        radius: Double = 1.5,
        steps: Int = 8,
    ) {
        val plugin = Bukkit.getServer().pluginManager.plugins[0]
        val yaw = Math.toRadians(location.yaw.toDouble())
        var step = 0
        val dustOptions = if (color != null) Particle.DustOptions(color, 1.0f) else null

        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                { task ->
                    if (step >= steps) {
                        task.cancel()
                        return@runTaskTimer
                    }
                    val angle = (step.toDouble() / steps) * Math.PI - Math.PI / 2
                    val x = radius * cos(yaw) * cos(angle)
                    val y = radius * sin(angle) + 1.0
                    val z = radius * sin(yaw) * cos(angle)
                    val loc = location.clone().add(x, y, z)
                    if (dustOptions != null) {
                        player.world.spawnParticle(
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
                        player.world.spawnParticle(particle, loc, 3, 0.05, 0.05, 0.05, 0.0)
                    }
                    step++
                },
                0L,
                1L,
            )
    }

    fun slashHorizontal(
        player: Player,
        particle: Particle,
        location: Location,
        color: Color? = null,
        radius: Double = 1.5,
        steps: Int = 8,
    ) {
        val plugin = Bukkit.getServer().pluginManager.plugins[0]
        val yaw = Math.toRadians(location.yaw.toDouble())
        var step = 0
        val dustOptions = if (color != null) Particle.DustOptions(color, 1.0f) else null

        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                { task ->
                    if (step >= steps) {
                        task.cancel()
                        return@runTaskTimer
                    }
                    val angle = (step.toDouble() / steps) * Math.PI * 2
                    val x = radius * cos(angle + yaw)
                    val z = radius * sin(angle + yaw)
                    val loc = location.clone().add(x, 1.0, z)
                    if (dustOptions != null) {
                        player.world.spawnParticle(
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
                        player.world.spawnParticle(particle, loc, 3, 0.05, 0.05, 0.05, 0.0)
                    }
                    step++
                },
                0L,
                1L,
            )
    }
}
