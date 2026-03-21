package com.runicrealms.game.gameplay.spell.spellutil.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity

/**
 * Creates a rotating helix cone around a [LivingEntity] using a Bukkit timer. Supports REDSTONE
 * (DustOptions), NOTE, BLOCK_CRACK, or generic particle. Auto-cancels after [DURATION] seconds.
 */
object Cone {

    private const val DURATION = 1.0

    fun coneEffect(entity: LivingEntity, particle: Particle, color: Color, scale: Double) {
        val world = entity.world
        val plugin = Bukkit.getServer().pluginManager.plugins[0]
        var ticks = 0
        val maxTicks = (DURATION * 20).toInt()
        var angle = 0.0

        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                { task ->
                    if (ticks >= maxTicks || !entity.isValid) {
                        task.cancel()
                        return@runTaskTimer
                    }
                    val loc = entity.location.add(0.0, 1.0, 0.0)
                    val radius = 0.5 * scale
                    for (i in 0..3) {
                        val a = angle + (PI / 2.0 * i)
                        val x = radius * cos(a)
                        val z = radius * sin(a)
                        val particleLoc = loc.clone().add(x, 0.0, z)
                        if (particle == Particle.DUST) {
                            world.spawnParticle(
                                Particle.DUST,
                                particleLoc,
                                1,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                Particle.DustOptions(color, 1.0f),
                            )
                        } else {
                            world.spawnParticle(particle, particleLoc, 1, 0.0, 0.0, 0.0, 0.0)
                        }
                    }
                    angle += 0.2
                    ticks++
                },
                0L,
                1L,
            )
    }
}
