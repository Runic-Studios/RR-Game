package com.runicrealms.game.items.listeners

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.items.command.InventoryHelper
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.plugin.Plugin

/**
 * Custom handler for shift-click (MOVE_TO_OTHER_INVENTORY) when the player's own
 * inventory is open. Moves items between hotbar and main inventory with smart stacking.
 */
class PlayerMoveToInventoryListener
@Inject
constructor(
    plugin: Plugin,
    private val inventoryHelper: InventoryHelper,
) : Listener {

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onMoveToOtherInventory(event: InventoryClickEvent) {
        if (event.action != InventoryAction.MOVE_TO_OTHER_INVENTORY) return
        // Only handle player's own inventory
        if (event.view.topInventory !is CraftingInventory) return
        event.clickedInventory ?: return
        val player = event.whoClicked as? Player ?: return
        if (event.isCancelled) return
        val currentItem = event.currentItem
        if (currentItem == null || currentItem.type == Material.AIR) return

        // If clicked from main inventory (slot >= 9), target hotbar (0-8)
        // If clicked from hotbar (slot < 9), target main inventory (9-35)
        val loopStart: Int
        val loopEnd: Int
        if (event.slot >= 9) {
            loopStart = 0
            loopEnd = 9
        } else {
            loopStart = 9
            loopEnd = player.inventory.size
        }

        inventoryHelper.combineItemStacks(
            player,
            event,
            currentItem,
            player.inventory,
            loopStart,
            loopEnd,
        )
    }
}
