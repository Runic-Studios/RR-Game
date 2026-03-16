package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class RefreshingVolley(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), HealingSpell, DistanceSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var healAmount = HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var distance = DISTANCE
    override var description =
        "While Rapid Fire is active, each ranged hit heals you and your closest 2 allies " +
            "within ${distance} blocks for (${healAmount} + ${healPerLevel}x lvl)."

    init {
        isPassive = true
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell.
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onRangedPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isRanged) return
        if (!hasPassive(event.caster.uniqueId, name)) return

        val rapidFire = spellManager.getSpell(RapidFire.SPELL_NAME) as? RapidFire ?: return
        if (!rapidFire.isUsing(event.caster)) return

        healPlayer(event.caster, event.caster, healAmount, this)

        val nearestAllies =
            event.caster.world
                .getNearbyEntities(event.caster.location, distance, distance, distance) {
                    it.uniqueId != event.caster.uniqueId && isValidAlly(event.caster, it)
                }
                .mapNotNull { it as? Player }
                .sortedBy { it.location.distanceSquared(event.caster.location) }
                .take(2)

        for (ally in nearestAllies) {
            healPlayer(event.caster, ally, healAmount, this)
        }
    }

    companion object {
        const val SPELL_NAME = "Refreshing Volley"
        const val HEAL = 14.0
        const val HEAL_PER_LEVEL = 1.0
        const val DISTANCE = 12.0
    }
}
