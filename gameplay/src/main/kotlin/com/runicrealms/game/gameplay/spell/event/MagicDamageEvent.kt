package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

/** Fired when a spell deals magic damage to an entity. */
class MagicDamageEvent(
    amount: Int,
    victim: LivingEntity,
    val caster: Player,
    val spell: Spell? = null,
    var isCritical: Boolean = false,
) : RunicDamageEvent(victim, amount) {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
