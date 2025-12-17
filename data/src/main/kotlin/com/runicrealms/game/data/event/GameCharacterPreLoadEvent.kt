package com.runicrealms.game.data.event

import com.runicrealms.trove.client.user.UserCharacterData
import java.util.UUID
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired after a player has chosen a character and we have loaded their data, but before we have
 * registered them as a character and fired GameCharacterLoadEvent.
 *
 * This event should be used for applying default values to NEW character data (when a player
 * creates a brand-new character).
 *
 * This event cannot be failed.
 */
class GameCharacterPreLoadEvent(val user: UUID, val characterData: UserCharacterData) :
    Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
