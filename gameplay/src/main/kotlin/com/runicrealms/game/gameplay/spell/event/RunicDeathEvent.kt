package com.runicrealms.game.gameplay.spell.event

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired when a player's health would drop to 0 or below. The vanilla death and respawn mechanics
 * are cancelled; this event drives all custom death logic instead.
 *
 * Cancelling this event (e.g. by an UndyingPerk) prevents death from occurring.
 *
 * @param victim the player who died
 * @param location where the player died (retained in case they disconnect before handling)
 * @param killer optional entity responsible for the kill
 */
class RunicDeathEvent(val victim: Player, val location: Location, val killer: Entity? = null) :
    Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
