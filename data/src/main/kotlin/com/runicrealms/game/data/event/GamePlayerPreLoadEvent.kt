package com.runicrealms.game.data.event

import com.runicrealms.trove.client.user.UserPlayerData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired after a player has logged-in and we have loaded their data, but before we have registered
 * them as a player and fired GamePlayerLoadEvent.
 *
 * This event should be used for applying default values to NEW player data (when a player logs in
 * for the first time).
 *
 * This event cannot be failed.
 */
class GamePlayerPreLoadEvent(val playerData: UserPlayerData) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
