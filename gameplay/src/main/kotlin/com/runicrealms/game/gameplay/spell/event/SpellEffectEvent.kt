package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired when a [SpellEffect] is added to the
 * [com.runicrealms.game.gameplay.spell.effect.SpellEffectManager]. Cancel to prevent the effect
 * from being applied.
 */
class SpellEffectEvent(val spellEffect: SpellEffect) : Event(), Cancellable {

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
