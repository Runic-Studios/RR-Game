package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.Circle
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

class SnareTrap(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps),
    DurationSpell,
    RadiusSpell,
    WarmupSpell,
    PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = ROOT_DURATION
    override var radius = RADIUS
    override var warmupSeconds = WARMUP
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    var trapDuration = TRAP_DURATION
    override var description =
        "Lay a trap that arms after ${warmupSeconds}s and lasts ${trapDuration}s. " +
            "The first enemy in ${radius} blocks takes physical damage and is rooted for ${duration}s."

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        duration = config.getDouble("stun-duration", duration)
        trapDuration = config.getDouble("trap-duration", trapDuration)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        val location = player.location
        val castLocation =
            Location(
                location.world,
                location.blockX + 0.5,
                location.blockY.toDouble(),
                location.blockZ + 0.5,
            )
        val hologramName = "snare_trap_${player.uniqueId}_${System.currentTimeMillis()}"
        val data = TextHologramData(hologramName, castLocation.clone().add(0.5, 1.0, 0.5))
        data.text = listOf("Snare Trap")
        data.isPersistent = false
        val manager = FancyHologramsPlugin.get().hologramManager
        val hologram = manager.create(data)
        manager.addHologram(hologram)

        player.world.playSound(player.location, Sound.BLOCK_ANVIL_PLACE, 0.5f, 2.0f)
        var count = 1
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count > trapDuration) {
                    task.cancel()
                    manager.removeHologram(hologram)
                    return@runTaskTimer
                }

                count += 1
                Circle.createParticleCircle(castLocation, Particle.CRIT, radius)
                var trapSprung = false
                for (entity in
                    player.world.getNearbyEntities(castLocation, radius, radius, radius)) {
                    val target = entity as? LivingEntity ?: continue
                    if (!isValidEnemy(player, target)) continue
                    trapSprung = true
                    springTrap(target, player)
                }

                if (trapSprung) {
                    task.cancel()
                    castLocation.world?.playSound(
                        castLocation,
                        Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,
                        0.25f,
                        1.0f,
                    )
                    castLocation.world?.playSound(
                        castLocation,
                        Sound.BLOCK_PORTAL_TRAVEL,
                        0.25f,
                        1.0f,
                    )
                    manager.removeHologram(hologram)
                }
            },
            (warmupSeconds * 20.0).toLong(),
            20L,
        )
    }

    private fun springTrap(target: LivingEntity, caster: Player) {
        val location = target.location.clone()
        target.world.spawnParticle(Particle.CRIT, location, 15, 0.25, 0.25, 0.25, 0.0)

        val dmgEvent =
            PhysicalDamageEvent(physicalDamage.toInt(), target, caster, false, true, this)
        Bukkit.getPluginManager().callEvent(dmgEvent)
        if (!dmgEvent.isCancelled) {
            target.damage(dmgEvent.amount.toDouble(), caster)
        }

        addStatusEffect(target, RunicStatusEffect.ROOT, duration, true)
        if (target !is Player) {
            val task =
                deps.plugin.server.scheduler.runTaskTimer(
                    deps.plugin,
                    Runnable { target.teleport(location) },
                    0L,
                    10L,
                )
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable { task.cancel() },
                (duration * 20.0).toLong(),
            )
        }
    }

    companion object {
        const val SPELL_NAME = "Snare Trap"
        const val COOLDOWN = 18.0
        const val MANA_COST = 25
        const val ROOT_DURATION = 2.0
        const val WARMUP = 1.0
        const val RADIUS = 3.0
        const val TRAP_DURATION = 8.0
        const val PHYSICAL_DAMAGE = 16.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
    }
}
