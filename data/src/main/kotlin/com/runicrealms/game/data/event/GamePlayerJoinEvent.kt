package com.runicrealms.game.data.event

import com.runicrealms.game.data.game.GamePlayer
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class GamePlayerJoinEvent(val player: GamePlayer) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
