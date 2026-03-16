package com.runicrealms.game.gameplay.spell.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired when a player leaves combat. */
class LeaveCombatEvent(val player: Player) : Event() {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
