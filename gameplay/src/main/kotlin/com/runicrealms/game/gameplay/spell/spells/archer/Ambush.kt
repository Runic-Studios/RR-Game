package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.RunicBowEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.EntityTrail
import java.util.UUID
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask

/**
 * Passive: sneaking without casting spells for [warmupSeconds]s arms an ambush shot. The next
 * ranged basic attack will critically strike and blind the target. Cannot trigger more than once
 * every [cooldown]s.
 */
class Ambush(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), DurationSpell, WarmupSpell {

    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = BLIND_DURATION
    override var warmupSeconds = WARMUP
    override var description =
        "Sneaking without casting spells for at least ${warmupSeconds}s causes your next ranged " +
            "basic attack (if it lands) to ambush its target. Ambush attacks critically strike " +
            "and blind your opponent for ${duration}s. Cannot occur more than once every ${cooldown}s."

    private val ambushPlayers: MutableSet<UUID> = mutableSetOf()
    private val cooldownPlayers: MutableSet<UUID> = mutableSetOf()
    private val successfulPlayers: MutableSet<UUID> = mutableSetOf()
    private val sneakMap: MutableMap<UUID, BukkitTask> = mutableMapOf()

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onCustomArrowHit(event: EntityDamageByEntityEvent) {
        if (!event.damager.hasMetadata(AMBUSH_ARROW_KEY)) return
        val uuid = UUID.fromString(event.damager.getMetadata(AMBUSH_ARROW_KEY)[0].asString())
        successfulPlayers.add(uuid)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onRangedPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isRanged || !event.isBasicAttack) return
        if (!hasPassive(event.caster.uniqueId, this.name)) return
        if (!successfulPlayers.contains(event.caster.uniqueId)) return
        successfulPlayers.remove(event.caster.uniqueId)
        event.victim.addPotionEffect(
            PotionEffect(PotionEffectType.BLINDNESS, (duration * 20).toInt(), 1)
        )
        event.isCritical = true
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onRunicBow(event: RunicBowEvent) {
        if (event.isCancelled) return
        if (!hasPassive(event.player.uniqueId, this.name)) return
        if (!ambushPlayers.contains(event.player.uniqueId)) return
        ambushPlayers.remove(event.player.uniqueId)
        event.arrow.setMetadata(
            AMBUSH_ARROW_KEY,
            FixedMetadataValue(deps.plugin, event.player.uniqueId.toString()),
        )
        EntityTrail.entityTrail(event.arrow, Particle.CLOUD, 5000L)
        cooldownPlayers.add(event.player.uniqueId)
        deps.plugin.server.scheduler.runTaskLaterAsynchronously(
            deps.plugin,
            Runnable { cooldownPlayers.remove(event.player.uniqueId) },
            (cooldown * 20).toLong(),
        )
    }

    @EventHandler
    fun onSpellCast(event: SpellCastEvent) {
        if (event.isCancelled) return
        if (!sneakMap.containsKey(event.caster.uniqueId)) return
        if (!hasPassive(event.caster.uniqueId, this.name)) return
        if (ambushPlayers.contains(event.caster.uniqueId)) return
        sneakMap[event.caster.uniqueId]?.cancel()
        sneakMap.remove(event.caster.uniqueId)
    }

    @EventHandler
    fun onToggleSneak(event: PlayerToggleSneakEvent) {
        if (event.isCancelled) return
        if (!hasPassive(event.player.uniqueId, this.name)) return
        if (ambushPlayers.contains(event.player.uniqueId)) return
        if (cooldownPlayers.contains(event.player.uniqueId)) return
        if (event.isSneaking) {
            event.player.playSound(event.player.location, Sound.ENTITY_TNT_PRIMED, 0.5f, 1.0f)
            val task =
                deps.plugin.server.scheduler.runTaskLater(
                    deps.plugin,
                    Runnable {
                        ambushPlayers.add(event.player.uniqueId)
                        event.player.playSound(
                            event.player.location,
                            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                            0.5f,
                            1.5f,
                        )
                        event.player.sendMessage("\u00a7aYour ambush attack is primed!")
                        sneakMap.remove(event.player.uniqueId)
                    },
                    (warmupSeconds * 20).toLong(),
                )
            sneakMap[event.player.uniqueId] = task
        } else {
            sneakMap[event.player.uniqueId]?.cancel()
            sneakMap.remove(event.player.uniqueId)
        }
    }

    companion object {
        const val SPELL_NAME = "Ambush"
        const val COOLDOWN = 30.0
        const val MANA_COST = 0
        const val BLIND_DURATION = 3.0
        const val WARMUP = 3.0
        const val AMBUSH_ARROW_KEY = "ambush"
    }
}
