package com.runicrealms.game.data.event

import com.runicrealms.game.data.game.GameCharacter
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fires SYNCHRONOUSLY after a player has chosen their character and we have fired
 * [GameCharacterLoadEvent] successfully, which applies the data to the Bukkit player.
 *
 * This event cannot be failed.
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
