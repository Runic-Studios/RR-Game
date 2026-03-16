package com.runicrealms.game.data.event

import com.runicrealms.game.data.model.PlayerDocument
import java.util.UUID
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired after a player has logged-in and we have loaded their document, but before we have
 * registered them as a player and fired [GamePlayerLoadEvent].
 *
 * Use this event to set default values for NEW player data. Check [PlayerDocument.isNewPlayer] to
 * determine whether this is the player's first ever login.
 *
 * This event cannot be failed. Mutate [document] directly (the full document is already in memory).
 * Changes will be picked up by the next periodic save automatically.
 */
class GamePlayerPreLoadEvent(val user: UUID, val document: PlayerDocument) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
