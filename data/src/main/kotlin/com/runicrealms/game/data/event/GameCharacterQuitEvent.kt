package com.runicrealms.game.data.event

import com.runicrealms.game.data.game.GameCharacter
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fires SYNCHRONOUSLY after a player quits their character, but just before we save and destroy
 * their data.
 */
class GameCharacterQuitEvent(val character: GameCharacter, val isOnLogout: Boolean) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
