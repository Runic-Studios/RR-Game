package com.runicrealms.game.gameplay.spell.event

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when a spell attempts to verify an entity as a valid enemy target. Cancel this event to
 * prevent the entity from taking damage (e.g. same-party members, non-pvp zones).
 */
class EnemyVerifyEvent(val caster: Player, val victim: Entity) : Event(), Cancellable {

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
