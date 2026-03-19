package com.runicrealms.game.gameplay.spell.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageEvent

/**
 * Fired in place of [EntityDamageEvent] when a player takes environment damage (fall, fire,
 * drowning, lava, etc.). The vanilla event is cancelled and this event is called instead, allowing
 * the custom damage pipeline to apply shields, resistances, and other effects.
 *
 * Set [damage] to 0 or cancel this event to negate the damage entirely.
 */
class EnvironmentDamageEvent(
    val player: Player,
    val cause: EntityDamageEvent.DamageCause,
    var damage: Double,
) : Event(), Cancellable {

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
