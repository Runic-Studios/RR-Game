package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

/** Fired when a player deals ranged physical damage via an arrow. */
class RangedDamageEvent(
    amount: Int,
    victim: LivingEntity,
    caster: Player,
    val arrow: Arrow,
    isBasicAttack: Boolean = false,
    spell: Spell? = null,
    isCritical: Boolean = false,
) : PhysicalDamageEvent(amount, victim, caster, isBasicAttack, isRanged = true, spell, isCritical) {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
