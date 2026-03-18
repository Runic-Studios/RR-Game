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
 * Custom handler for shift-click (MOVE_TO_OTHER_INVENTORY) when the player has a
 * non-player inventory open (chest, shop, etc.). Uses smart stacking via [InventoryHelper].
 *
 * Player's own inventory is handled by [PlayerMoveToInventoryListener].
 */
class MoveToInventoryListener
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
        // Don't handle player's own inventory (CraftingInventory is the default top inventory)
        if (event.view.topInventory is CraftingInventory) return
        val clickedInventory = event.clickedInventory ?: return
        val player = event.whoClicked as? Player ?: return
        if (event.isCancelled) return
        val currentItem = event.currentItem
        if (currentItem == null || currentItem.type == Material.AIR) return

        val targetInventory = if (clickedInventory == event.view.topInventory) {
            event.view.bottomInventory
        } else {
            event.view.topInventory
        }

        inventoryHelper.combineItemStacks(player, event, currentItem, targetInventory)
    }
}
