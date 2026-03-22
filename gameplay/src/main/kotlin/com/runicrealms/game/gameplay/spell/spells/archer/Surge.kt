package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import java.util.UUID
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

class Surge(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.ARCHER, deps), DurationSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    var speedMultiplier = SPEED_MULTIPLIER
    override val description: String
        get() =
            "Launch yourself forward. Landing a Stormborn arrow reduces this spell cooldown by ${duration}s."

    private val surgeTasks: MutableMap<UUID, BukkitTask> = HashMap()

    override fun loadSpellSpecificData(config: FileConfiguration) {
        speedMultiplier = config.getDouble("speed-multiplier", speedMultiplier)
        super.loadSpellSpecificData(config)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        val trailSpots = HashSet<Location>()
        val trailTask =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    trailSpots.add(player.location.clone())
                    for (location in trailSpots) {
                        player.world.spawnParticle(
                            Particle.DUST,
                            location,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            Particle.DustOptions(Color.fromRGB(0, 71, 72), 3.0f),
                        )
                    }
                },
                0L,
                5L,
            )

        val surgeTask =
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable {
                    surgeTasks.remove(player.uniqueId)
                    trailTask.cancel()
                },
                (DURATION_FALL * 20.0).toLong(),
            )

        player.world.playSound(player.location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f)
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.8f)
        var unitVector = Vector(player.location.direction.x, 0.0, player.location.direction.z)
        unitVector = unitVector.normalize().multiply(speedMultiplier)
        player.velocity = unitVector
        surgeTasks[player.uniqueId] = surgeTask
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onFallDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!surgeTasks.containsKey(player.uniqueId)) return
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onStormbornArrowHit(event: Stormborn.Companion.ArrowHitEvent) {
        if (!spellManager.isOnCooldown(event.caster, name)) return
        spellManager.reduceCooldown(event.caster, name, duration)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        surgeTasks.remove(event.player.uniqueId)?.cancel()
    }

    companion object {
        const val SPELL_NAME = "Surge"
        const val COOLDOWN = 14.0
        const val MANA_COST = 25
        const val DURATION = 2.0
        const val SPEED_MULTIPLIER = 2.0
        const val DURATION_FALL = 2.5
    }
}
