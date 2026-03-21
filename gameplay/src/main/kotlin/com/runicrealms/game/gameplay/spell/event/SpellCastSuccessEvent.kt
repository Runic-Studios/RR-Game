package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired after a spell executes successfully (all pre-conditions passed, mana deducted, cooldown set). */
class SpellCastSuccessEvent(
    val caster: Player,
    val spell: Spell,
) : Event() {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
