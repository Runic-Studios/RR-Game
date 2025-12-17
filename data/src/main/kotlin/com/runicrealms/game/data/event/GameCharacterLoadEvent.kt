package com.runicrealms.game.data.event

import com.runicrealms.game.data.game.GameCharacter
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fires SYNCHRONOUSLY after:
 * - player selects a character
 * - we load character data (successfully)
 *
 * This event can be "failed", which will kick the player and not fire the GameCharacterJoinEvent
 */
class GameCharacterLoadEvent(val character: GameCharacter) : Event(false) {

    internal var success = true
    internal val errors by lazy { HashSet<Throwable>() }

    fun fail(throwable: Throwable) {
        success = false
        errors.add(throwable)
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
