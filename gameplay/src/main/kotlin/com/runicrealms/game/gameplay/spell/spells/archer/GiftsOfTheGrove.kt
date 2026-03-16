package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class GiftsOfTheGrove(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), AttributeSpell, DurationSpell {
    override var cooldown = COOLDOWN
    override var manaCost = 0
    override var attribute = ATTRIBUTE
    override var attributeBaseValue = ATTRIBUTE_BASE_VALUE
    override var attributeMultiplier = ATTRIBUTE_MULTIPLIER
    override var duration = DURATION
    var percent = PERCENT
    override var description =
        "While inside your Sacred Grove, your healing is increased by " +
            "(${attributeBaseValue} + ${attributeMultiplier}x attribute)%! " +
            "When you hit an enemy while inside the grove, Remedy cooldown is reduced by ${duration}s. " +
            "If you are in the grove when it expires, one final pulse heals allies for ${(percent * 100).toInt()}%."

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell.
    }

    @EventHandler
    fun onGroveExpiry(event: SacredGrove.Companion.GroveExpiryEvent) {
        if (event.isCancelled) return
        val groveLocation = SacredGrove.groveLocationMap[event.caster.uniqueId] ?: return
        val sacredGrove = spellManager.getSpell(SacredGrove.SPELL_NAME) as? SacredGrove ?: return
        val sacredRadius = sacredGrove.radius
        if (event.caster.location.distanceSquared(groveLocation) > sacredRadius * sacredRadius)
            return

        val healTotal = sacredGrove.healAmount + (sacredGrove.healPerLevel * event.caster.level)
        for (entity in
            event.caster.world.getNearbyEntities(
                groveLocation,
                sacredRadius,
                sacredRadius,
                sacredRadius,
            ) {
                isValidAlly(event.caster, it)
            }) {
            val ally = entity as? Player ?: continue
            healPlayer(event.caster, ally, percent * healTotal, this)
        }
    }

    @EventHandler
    fun onSpellHeal(event: SpellHealEvent) {
        if (event.isCancelled) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (event.spell !is SacredGrove) return
        val groveLocation = SacredGrove.groveLocationMap[event.caster.uniqueId] ?: return
        val sacredGrove = spellManager.getSpell(SacredGrove.SPELL_NAME) as? SacredGrove ?: return
        val sacredRadius = sacredGrove.radius
        if (event.caster.location.distanceSquared(groveLocation) > sacredRadius * sacredRadius)
            return

        // TODO: StatAPI pending (SPELL_MIGRATION.md #3)
        val bonus = attributeBaseValue / 100.0
        event.amount = (event.amount + (event.amount * bonus)).toInt()
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onRangedPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isRanged) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (!spellManager.isOnCooldown(event.caster, Remedy.SPELL_NAME)) return

        val groveLocation = SacredGrove.groveLocationMap[event.caster.uniqueId] ?: return
        val sacredGrove = spellManager.getSpell(SacredGrove.SPELL_NAME) as? SacredGrove ?: return
        val sacredRadius = sacredGrove.radius
        if (event.caster.location.distanceSquared(groveLocation) > sacredRadius * sacredRadius)
            return

        spellManager.reduceCooldown(event.caster, Remedy.SPELL_NAME, duration)
    }

    companion object {
        const val SPELL_NAME = "Gifts Of The Grove"
        const val COOLDOWN = 0.0
        const val ATTRIBUTE = "dexterity"
        const val ATTRIBUTE_BASE_VALUE = 10.0
        const val ATTRIBUTE_MULTIPLIER = 0.0
        const val DURATION = 1.5
        const val PERCENT = 0.5
    }
}
