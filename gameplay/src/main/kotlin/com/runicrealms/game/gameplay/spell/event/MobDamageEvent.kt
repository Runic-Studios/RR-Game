package com.runicrealms.game.gameplay.spell.event

import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.HandlerList

/** Fired when a mob deals damage to a player. */
class MobDamageEvent(
    victim: LivingEntity,
    amount: Int,
    val mob: Entity,
    var applyMechanics: Boolean = true,
) : RunicDamageEvent(victim, amount) {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
