package com.runicrealms.game.gameplay.spell.event

import java.util.UUID
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Superclass of all custom damage event mechanics on RunicRealms. */
abstract class RunicDamageEvent(val victim: LivingEntity, var amount: Int) : Event(), Cancellable {

    val eventId: UUID = UUID.randomUUID()
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
