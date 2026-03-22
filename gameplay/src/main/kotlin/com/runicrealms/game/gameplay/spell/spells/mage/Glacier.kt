package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.mage.IceBarrierEffect
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive. Increases IceBarrier max stacks to [maxStacks]. At max stacks, slows attackers (via
 * PhysicalDamageEvent or MobDamageEvent) for [duration] seconds.
 */
class Glacier(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.MAGE, deps), DurationSpell {

    override var duration = SLOW_DURATION
    override var cooldown = 0.0
    override var manaCost = 0
    var maxStacks = MAX_STACKS
    override val description: String
        get() = "Passive: Increases Ice Barrier max stacks to $maxStacks."

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        val victim = event.victim
        if (victim !is Player) return
        if (!hasSpellEffect(victim.uniqueId, SpellEffectType.ICE_BARRIER)) return

        val barrierEffects =
            deps.spellEffectAPI.getSpellEffects(victim.uniqueId, SpellEffectType.ICE_BARRIER)
        val barrier = barrierEffects.filterIsInstance<IceBarrierEffect>().firstOrNull() ?: return
        if (barrier.stacks.get() >= barrier.getMaxStacks()) {
            addStatusEffect(event.caster, RunicStatusEffect.SLOW_I, duration, false)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onMobDamage(event: MobDamageEvent) {
        val victim = event.victim as? Player ?: return
        if (!hasSpellEffect(victim.uniqueId, SpellEffectType.ICE_BARRIER)) return

        val barrierEffects =
            deps.spellEffectAPI.getSpellEffects(victim.uniqueId, SpellEffectType.ICE_BARRIER)
        val barrier = barrierEffects.filterIsInstance<IceBarrierEffect>().firstOrNull() ?: return
        if (barrier.stacks.get() >= barrier.getMaxStacks()) {
            // Mobs can't be slowed via RunicStatusEffect, so we skip that
        }
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        maxStacks = loadInt(config, "max-stacks", maxStacks)
    }

    companion object {
        const val SPELL_NAME = "Glacier"
        const val MAX_STACKS = 5
        const val SLOW_DURATION = 2.0
    }
}
