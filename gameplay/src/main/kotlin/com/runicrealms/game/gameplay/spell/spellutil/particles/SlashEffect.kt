package com.runicrealms.game.gameplay.spell.spellutil.particles

import java.util.concurrent.ThreadLocalRandom
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

    /**
     * Creates a diagonal slash sweep of particles in front of the player. Particles fan out from
     * one side to the other using randomised direction, matching the original system's behaviour.
     */
    fun slashHorizontal(
        player: Player,
        particle: Particle,
        location: Location,
        color: Color? = null,
        radius: Double = 1.5,
        steps: Int = 8,
    ) {
        val plugin = Bukkit.getServer().pluginManager.plugins[0]
        val topOrBottom = ThreadLocalRandom.current().nextBoolean()
        val leftOrRight = ThreadLocalRandom.current().nextBoolean()
        val loc = location.clone()
        if (loc.pitch > 60f || loc.pitch < -60f) loc.pitch = 0f
        loc.add(0.0, 1.0, 0.0)
        val direction = if (topOrBottom) -1 else 1
        val dustOptions = if (color != null) Particle.DustOptions(color, 1.0f) else null

        val density = 0.05
        var count = 0
        var timer = 0

        var i = if (leftOrRight) -1.0 else 1.0
        val stepSize = if (leftOrRight) density else -density

        while (if (leftOrRight) i < 1.0 else i > -1.0) {
            val fi = i
            val fd = direction
            Bukkit.getScheduler()
                .runTaskLater(
                    plugin,
                    Runnable {
                        val dir = loc.direction.clone().multiply(2.0)
                        dir.rotateAroundY(fi)
                        val spawnLoc = loc.clone().add(dir).add(0.0, fi / 2.0 * fd, 0.0)
                        if (dustOptions != null) {
                            spawnLoc.world?.spawnParticle(
                                Particle.DUST,
                                spawnLoc,
                                0,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                dustOptions,
                            )
                        } else {
                            spawnLoc.world?.spawnParticle(particle, spawnLoc, 0, 0.0, 0.0, 0.0, 0.0)
                        }
                    },
                    timer.toLong(),
                )
            count++
            if (count > 10) {
                timer++
                count = 0
            }
            i += stepSize
        }
    }
}
