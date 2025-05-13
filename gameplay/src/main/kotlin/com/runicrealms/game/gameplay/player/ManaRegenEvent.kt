package com.runicrealms.game.gameplay.player

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ManaRegenEvent(val player: Player, var amount: Int) : Event(false), Cancellable {

    private var isCancelled = false

    override fun isCancelled(): Boolean {
        return this.isCancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        this.isCancelled = cancelled
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
