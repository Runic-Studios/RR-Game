package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.util.Vector

class Rejuvenate(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, HealingSpell, RadiusSpell {
    override var duration = BASE_DURATION
    override var healAmount = BASE_HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() =
            "Launch a healing beam. Allies hit receive bonus healing from your spells for $duration seconds."

    private val hasBeenHit: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf()
    private val affectedPlayers: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf()

    override fun executeSpell(player: Player, type: SpellItemType) {
        healPlayer(player, player, healAmount, this)
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f)
        val middle = player.eyeLocation.direction.normalize().multiply(BEAM_SPEED)
        startTask(player, middle)
    }

    @EventHandler
    fun onHeal(event: SpellHealEvent) {
        val caster = event.caster
        if (!affectedPlayers.containsKey(caster.uniqueId)) return
        if (!affectedPlayers[caster.uniqueId]!!.contains(event.recipient.uniqueId)) return
        var extra = (event.amount * BONUS_PERCENT).toInt()
        if (extra < 1) extra = 1
        event.amount += extra
    }

    private fun allyCheck(caster: Player, location: Location): Boolean {
        val allies = affectedPlayers.getOrPut(caster.uniqueId) { mutableSetOf() }
        for (entity in caster.world.getNearbyEntities(location, radius, radius, radius)) {
            val ally = entity as? Player ?: continue
            if (!isValidAlly(caster, ally)) continue

            val casters = hasBeenHit.getOrPut(ally.uniqueId) { mutableSetOf() }
            if (casters.contains(caster.uniqueId)) break
            casters.add(caster.uniqueId)
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable { hasBeenHit[ally.uniqueId]?.remove(caster.uniqueId) },
                SUCCESSIVE_COOLDOWN * 20L,
            )

            if (ally.health >= (ally.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0)) {
                ally.sendMessage("${caster.name} tried to heal you, but you are at full health.")
                ally.playSound(caster.location, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f)
            } else {
                healPlayer(caster, ally, healAmount, this)
                caster.playSound(caster.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f)
                allies.add(ally.uniqueId)
                deps.plugin.server.scheduler.runTaskLater(
                    deps.plugin,
                    Runnable { affectedPlayers.remove(caster.uniqueId) },
                    (duration * 20).toLong(),
                )
                return true
            }
        }
        return false
    }

    private fun startTask(player: Player, vector: Vector) {
        val location = player.eyeLocation.clone()
        val startLoc = player.location.clone()
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                location.add(vector)
                if (
                    location.block.type.isSolid ||
                        location.distanceSquared(startLoc) >= RANGE * RANGE
                ) {
                    task.cancel()
                    player.world.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        location,
                        15,
                        0.5,
                        0.5,
                        0.5,
                        0.0,
                    )
                    return@runTaskTimer
                }
                player.world.spawnParticle(
                    Particle.DUST,
                    location,
                    10,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    Particle.DustOptions(Color.LIME, 1.0f),
                )
                player.world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    location,
                    10,
                    0.1,
                    0.1,
                    0.1,
                    0.0,
                )
                if (allyCheck(player, location)) {
                    task.cancel()
                }
            },
            0L,
            1L,
        )
    }

    companion object {
        const val SPELL_NAME = "Rejuvenate"
        private const val BASE_DURATION = 6.0
        private const val BASE_HEAL = 16.0
        private const val HEAL_PER_LEVEL = 0.4
        private const val BASE_RADIUS = 1.25
        private const val COOLDOWN = 10.0
        private const val MANA_COST = 22
        private const val RANGE = 15
        private const val BEAM_SPEED = 3
        private const val SUCCESSIVE_COOLDOWN = 2L
        private const val BONUS_PERCENT = 0.35
    }
}
