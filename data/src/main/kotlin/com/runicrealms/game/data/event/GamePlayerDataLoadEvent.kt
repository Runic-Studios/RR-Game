package com.runicrealms.game.data.event

import com.runicrealms.trove.client.user.UserPlayerData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired after a player has logged-in and we have loaded their data, but before we have registered
 * them as a player and fired GamePlayerJoinEvent
 */
class GamePlayerDataLoadEvent(val playerData: UserPlayerData) : Event(true) {

    companion object {
        private val HANDLERS: HandlerList = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
