package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.RunicDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent

class Accelerando(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps),
    DurationSpell,
    RadiusSpell,
    AttributeSpell,
    Tempo.Influenced {
    override var duration = BASE_DURATION
    override var radius = BASE_RADIUS
    override var attribute = "intelligence"
    override var attributeBaseValue = BASE_REDUCTION
    override var attributeMultiplier = BASE_MULTIPLIER
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Whenever you cast a cleric battle spell, nearby allies gain Speed II and damage reduction for $duration seconds."

    private val damageReductionData: MutableMap<UUID, ReductionData> = mutableMapOf()

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive - handled through event listeners.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        val caster = event.caster
        if (!hasPassive(caster.uniqueId, name)) return
        if (
            event.spell !is Battlecry && event.spell !is Powerslide && event.spell !is GrandSymphony
        )
            return

        val now = System.currentTimeMillis()
        val percent = percentAttribute(caster)
        val buffDuration = effectiveDuration(caster)
        val expiresAt = now + (buffDuration * 1000.0).toLong()

        applySpeed(caster, caster, buffDuration)
        damageReductionData[caster.uniqueId] = ReductionData(percent, expiresAt)

        for (entity in caster.getNearbyEntities(radius, radius, radius)) {
            val ally = entity as? Player ?: continue
            if (!isValidAlly(caster, ally)) continue
            applySpeed(caster, ally, buffDuration)
            damageReductionData[ally.uniqueId] = ReductionData(percent, expiresAt)
        }

        removeExtraDuration(caster)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        reduceDamage(event.victim.uniqueId, event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        reduceDamage(event.victim.uniqueId, event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        val victim = event.victim as? Player ?: return
        reduceDamage(victim.uniqueId, event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEnvironmentDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val data = damageReductionData[player.uniqueId] ?: return
        if (System.currentTimeMillis() > data.expiresAtMs) {
            damageReductionData.remove(player.uniqueId)
            return
        }
        val reduction = event.damage * data.percent
        event.damage = (event.damage - reduction).coerceAtLeast(0.0)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        damageReductionData.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onBasicAttack(event: BasicAttackEvent) {
        if (!damageReductionData.containsKey(event.player.uniqueId)) return
        // Keep passive state active while buff duration is valid.
    }

    private fun reduceDamage(victimId: UUID, event: RunicDamageEvent) {
        val victim = event.victim as? Player ?: return
        val data = damageReductionData[victimId] ?: return
        if (System.currentTimeMillis() > data.expiresAtMs) {
            damageReductionData.remove(victimId)
            removeExtraDuration(victim)
            return
        }
        val reduced = (event.amount * data.percent).toInt()
        event.amount = (event.amount - reduced).coerceAtLeast(0)
    }

    private fun applySpeed(caster: Player, target: Player, buffDuration: Double) {
        target.world.playSound(target.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.7f)
        addStatusEffect(target, RunicStatusEffect.SPEED_II, buffDuration, false)
        target.world.spawnParticle(
            Particle.DUST,
            target.location,
            25,
            0.5,
            0.5,
            0.5,
            0.0,
            Particle.DustOptions(Color.WHITE, 1.0f),
        )
    }

    private fun percentAttribute(player: Player): Double {
        // TODO: include StatAPI scaling (attributeMultiplier * playerStat) when StatAPI is
        // migrated.
        return (attributeBaseValue / 100.0).coerceAtLeast(0.0)
    }

    companion object {
        const val SPELL_NAME = "Accelerando"
        private const val COOLDOWN = 0.0
        private const val MANA_COST = 0
        private const val BASE_DURATION = 4.0
        private const val BASE_RADIUS = 8.0
        private const val BASE_REDUCTION = 10.0
        private const val BASE_MULTIPLIER = 0.0
    }

    override fun increaseExtraDuration(player: Player, seconds: Double) {
        super<Tempo.Influenced>.increaseExtraDuration(player, seconds)
        val currentDuration =
            deps.statusEffectAPI.getStatusEffectDuration(
                player.uniqueId,
                RunicStatusEffect.SPEED_II,
            )
        if (currentDuration > 0.0) {
            addStatusEffect(player, RunicStatusEffect.SPEED_II, currentDuration + seconds, false)
        }
    }

    private data class ReductionData(val percent: Double, val expiresAtMs: Long)
}
