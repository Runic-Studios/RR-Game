package com.runicrealms.game.gameplay.spell.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired when a player enters combat. */
class EnterCombatEvent(val player: Player, val combatType: CombatType) : Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}

enum class CombatType(val durationSeconds: Int) {
    PVP(15),
    PVE(8),
}
