package com.runicrealms.game.gameplay.spell.event

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when a spell attempts to verify an entity as a valid ally target. Cancel this event to
 * prevent the entity from being healed/buffed (e.g. enemy outlaws, duel targets, etc.).
 */
class AllyVerifyEvent(val caster: Player, val recipient: Entity) : Event(), Cancellable {

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
