package com.runicrealms.game.gameplay.spell.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired when a Mage fires a particle bolt for their basic attack.
 *
 * TODO: Replace runicItemWeapon with the new items module weapon type once items are migrated.
 */
class StaffAttackEvent(val player: Player, val range: Int) : Event(), Cancellable {

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
