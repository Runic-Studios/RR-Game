package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired just before a spell executes. Cancel to prevent the cast. */
class SpellCastEvent(
    val caster: Player,
    var spell: Spell,
    val recipients: Array<out Entity> = emptyArray(),
) : Event(), Cancellable {

    private var cancelled = false

    /**
     * Secondary gate for external systems (e.g. tier sets) that want to suppress the cast without
     * technically cancelling the event itself.
     */
    var willExecute: Boolean = true

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
