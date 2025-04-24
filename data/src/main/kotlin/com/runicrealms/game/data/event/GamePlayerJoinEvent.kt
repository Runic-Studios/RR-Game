package com.runicrealms.game.data.event

import com.runicrealms.game.data.GameSession
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class GamePlayerJoinEvent(val session: GameSession) : Event(true) {

    companion object {
        private val HANDLERS: HandlerList = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
