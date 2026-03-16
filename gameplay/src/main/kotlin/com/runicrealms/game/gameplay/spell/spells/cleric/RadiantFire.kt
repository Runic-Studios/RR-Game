package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.cleric.RadiantFireEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler

class RadiantFire(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), AttributeSpell, DurationSpell {
    override var attribute = "wisdom"
    override var attributeBaseValue = BASE_ATTRIBUTE
    override var attributeMultiplier = BASE_MULTIPLIER
    override var duration = BASE_STACK_DURATION
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Landing Sear grants Radiant Fire stacks. Healing spells are stronger per stack."

    var maxStacks = BASE_MAX_STACKS
    var stackThreshold = BASE_STACK_THRESHOLD

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive - handled through event listeners.
    }

    @EventHandler
    fun onSpellCast(event: MagicDamageEvent) {
        if (event.isCancelled) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (event.spell !is Sear) return
        attemptToStack(event)
    }

    @EventHandler
    fun onSpellHeal(event: SpellHealEvent) {
        if (event.isCancelled) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (event.spell !is HealingSpell) return
        val effectOpt =
            getSpellEffect(
                event.caster.uniqueId,
                event.caster.uniqueId,
                SpellEffectType.RADIANT_FIRE,
            )
        if (effectOpt.isEmpty) return
        val effect = effectOpt.get() as? RadiantFireEffect ?: return
        val stacks = effect.stacks.get()
        val bonus = percentAttribute(event.caster) * stacks
        event.amount = (event.amount + (event.amount * bonus)).toInt()
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        duration = config.getDouble("stack-duration", duration)
        maxStacks = config.getDouble("max-stacks", maxStacks)
        stackThreshold = config.getDouble("stack-threshold", stackThreshold)
    }

    private fun attemptToStack(event: MagicDamageEvent) {
        val player = event.caster
        val effectOpt =
            getSpellEffect(player.uniqueId, player.uniqueId, SpellEffectType.RADIANT_FIRE)
        if (effectOpt.isEmpty) {
            RadiantFireEffect(
                    caster = player,
                    duration = duration,
                    maxStacks = maxStacks.toInt(),
                    stackThreshold = stackThreshold.toInt(),
                    spellEffectAPI = deps.spellEffectAPI,
                )
                .apply {
                    initialize()
                    increment()
                }
            return
        }
        val effect = effectOpt.get() as? RadiantFireEffect ?: return
        effect.increment()
    }

    private fun percentAttribute(player: Player): Double {
        // TODO: include StatAPI scaling (attributeMultiplier * playerStat) when StatAPI is
        // migrated.
        return (attributeBaseValue / 100.0).coerceAtLeast(0.0)
    }

    companion object {
        const val SPELL_NAME = "Radiant Fire"
        private const val BASE_ATTRIBUTE = 5.0
        private const val BASE_MULTIPLIER = 0.0
        private const val BASE_STACK_DURATION = 12.0
        private const val BASE_MAX_STACKS = 5.0
        private const val BASE_STACK_THRESHOLD = 4.0
    }
}
