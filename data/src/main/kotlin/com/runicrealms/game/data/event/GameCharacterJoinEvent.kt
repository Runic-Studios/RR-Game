package com.runicrealms.game.data.event

import com.runicrealms.game.data.GameSession
import com.runicrealms.trove.client.user.UserCharacterData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class GameCharacterJoinEvent(val session: GameSession, val characterData: UserCharacterData) :
    Event(true) {

    companion object {
        private val HANDLERS: HandlerList = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
