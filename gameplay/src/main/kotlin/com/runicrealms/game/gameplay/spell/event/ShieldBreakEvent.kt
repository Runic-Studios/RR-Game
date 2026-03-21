package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.spelltypes.ShieldPayload
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired when a player's spell shield is broken. */
class ShieldBreakEvent(val shieldPayload: ShieldPayload, val breakReason: BreakReason) :
    Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    enum class BreakReason {
        DAMAGE,
        FALLOFF,
    }

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
