package com.runicrealms.game.data.event

import com.runicrealms.game.data.game.GameCharacter
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired SYNCHRONOUSLY after:
 * - player selects a character
 * - we load character data (successfully)
 * - we fire GameCharacterLoadEvent (successfully), which applies the data to the bukkit player
 */
class GameCharacterJoinEvent(val character: GameCharacter) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
