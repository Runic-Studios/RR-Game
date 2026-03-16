package com.runicrealms.game.items.event

import com.runicrealms.game.items.config.item.GameItemClickTrigger
import com.runicrealms.game.items.generator.GameItemGeneric
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

class GameItemGenericTriggerEvent(
    val player: Player,
    val item: GameItemGeneric,
    val itemStack: ItemStack,
    val trigger: GameItemClickTrigger,
) : Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}
