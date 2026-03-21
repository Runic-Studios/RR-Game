package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

/** Fired when a player deals physical damage to an entity (basic attack or physical spell). */
open class PhysicalDamageEvent(
    amount: Int,
    victim: LivingEntity,
    val caster: Player,
    val isBasicAttack: Boolean = false,
    val isRanged: Boolean = false,
    val spell: Spell? = null,
    var isCritical: Boolean = false,
) : RunicDamageEvent(victim, amount) {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
