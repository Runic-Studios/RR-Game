package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.scheduler.BukkitTask

/** Passive: after casting a spell, next basic attack burns and reduces incoming healing. */
class Castigate(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell, MagicDamageSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var duration = DURATION_TO_HIT
    override var magicDamage = MAGIC_DAMAGE
    override var magicDamagePerLevel = MAGIC_DAMAGE_PER_LEVEL
    private var healingReductionDuration = HEALING_REDUCTION_DURATION
    private var numberOfTicks = NUMBER_OF_TICKS
    private var percent = HEAL_REDUCTION_PERCENT
    override var description =
        "After casting a spell, your next basic attack within $DURATION_TO_HIT s burns the target for " +
            "($MAGIC_DAMAGE + &f${MAGIC_DAMAGE_PER_LEVEL}x&7 lvl) magicʔ damage per second for $NUMBER_OF_TICKS s. " +
            "For $HEALING_REDUCTION_DURATION s, the target receives ${(HEAL_REDUCTION_PERCENT * 100).toInt()}% less healing."

    // Internal map-based state (SPELL_MIGRATION.md): caster -> buff timeout task
    private val buffedMap: ConcurrentHashMap<UUID, BukkitTask> = ConcurrentHashMap()
    // Internal map-based state (SPELL_MIGRATION.md): victim -> weaken expiry timestamp
    private val weakenedTargets: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        duration = config.getDouble("duration-to-hit", duration)
        healingReductionDuration =
            config.getDouble("healing-reduction-duration", healingReductionDuration)
        numberOfTicks = config.getDouble("number-of-ticks", numberOfTicks)
        percent = config.getDouble("percent", percent)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onHeal(event: SpellHealEvent) {
        val expiry = weakenedTargets[event.recipient.uniqueId] ?: return
        if (System.currentTimeMillis() > expiry) {
            weakenedTargets.remove(event.recipient.uniqueId)
            return
        }

        val reduction = (event.amount * percent).toInt()
        event.amount = (event.amount - reduction).coerceAtLeast(0)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        buffedMap.remove(event.caster.uniqueId)?.cancel()
        val task =
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable { buffedMap.remove(event.caster.uniqueId) },
                (duration * 20).toLong(),
            )
        buffedMap[event.caster.uniqueId] = task
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBasicAttack(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (!buffedMap.containsKey(event.caster.uniqueId)) return
        buffedMap.remove(event.caster.uniqueId)?.cancel()
        applyCastigation(event.caster, event.victim)
    }

    private fun applyCastigation(caster: Player, victim: LivingEntity) {
        weakenedTargets[victim.uniqueId] =
            System.currentTimeMillis() + (healingReductionDuration * 1000L).toLong()

        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                val state = castigationTicks.computeIfAbsent(victim.uniqueId) { 0 }
                if (state >= numberOfTicks.toInt() || !victim.isValid || victim.isDead) {
                    castigationTicks.remove(victim.uniqueId)
                    task.cancel()
                    return@runTaskTimer
                }

                castigationTicks[victim.uniqueId] = state + 1
                victim.world.spawnParticle(Particle.SOUL, victim.location, 5, 0.5, 0.5, 0.5, 0.0)
                victim.world.playSound(victim.location, Sound.ENTITY_WITCH_HURT, 0.25f, 0.5f)

                val damageEvent = MagicDamageEvent(magicDamage.toInt(), victim, caster, this)
                Bukkit.getPluginManager().callEvent(damageEvent)
                if (!damageEvent.isCancelled) {
                    victim.damage(damageEvent.amount.toDouble(), caster)
                }
            },
            0L,
            20L,
        )
    }

    companion object {
        const val SPELL_NAME = "Castigate"
        const val DURATION_TO_HIT = 5.0
        const val MAGIC_DAMAGE = 6.0
        const val MAGIC_DAMAGE_PER_LEVEL = 0.5
        const val NUMBER_OF_TICKS = 5.0
        const val HEALING_REDUCTION_DURATION = 6.0
        const val HEAL_REDUCTION_PERCENT = 0.5

        private val castigationTicks: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap()
    }
}
