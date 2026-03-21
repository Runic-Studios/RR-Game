package com.runicrealms.game.gameplay.spell.spellutil.particles

import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.Entity

private data class ParticleData(val particle: Particle, val color: Color?, val expiryMs: Long)

/**
 * Maintains a ConcurrentHashMap of entities with active particle trails. An async scheduler loop
 * spawns trail particles every tick; entities are auto-removed on death or duration expiry.
 *
 * Original author: BoBoBalloon (from the old codebase).
 */
object EntityTrail {

    private val trails: ConcurrentHashMap<Entity, ParticleData> = ConcurrentHashMap()
    private var started = false

    fun entityTrail(entity: Entity, particle: Particle, durationMs: Long, color: Color? = null) {
        trails[entity] = ParticleData(particle, color, System.currentTimeMillis() + durationMs)
        startIfNeeded()
    }

    fun removeTrail(entity: Entity) {
        trails.remove(entity)
    }

    private fun startIfNeeded() {
        if (started) return
        started = true
        val plugin = Bukkit.getServer().pluginManager.plugins[0]
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                plugin,
                Runnable {
                    val now = System.currentTimeMillis()
                    val toRemove = mutableListOf<Entity>()
                    for ((entity, data) in trails) {
                        if (!entity.isValid || now > data.expiryMs) {
                            toRemove.add(entity)
                            continue
                        }
                        val loc = entity.location
                        if (data.color != null && data.particle == Particle.DUST) {
                            entity.world.spawnParticle(
                                Particle.DUST,
                                loc,
                                1,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                Particle.DustOptions(data.color, 1.0f),
                            )
                        } else {
                            entity.world.spawnParticle(data.particle, loc, 1, 0.05, 0.05, 0.05, 0.0)
                        }
                    }
                    toRemove.forEach { trails.remove(it) }
                },
                0L,
                1L,
            )
    }
}
