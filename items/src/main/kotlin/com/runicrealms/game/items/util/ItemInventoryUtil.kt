package com.runicrealms.game.items.util

import com.runicrealms.game.items.generator.ItemStackConverter
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Utility functions for common item inventory operations: similarity checking, counting, taking,
 * and adding items.
 */
object ItemInventoryUtil {

    /**
     * Checks whether two ItemStacks represent the same game item template (ignoring count).
     * Non-game items (no CBOR data) are never considered similar.
     */
    fun isSimilar(converter: ItemStackConverter, first: ItemStack, second: ItemStack): Boolean {
        val firstData = converter.generateItemData(first) ?: return false
        val secondData = converter.generateItemData(second) ?: return false
        return firstData.templateID == secondData.templateID
    }

    /**
     * Checks if the player has at least [needed] of an item matching the same template as
     * [itemStack].
     */
    fun hasItem(
        converter: ItemStackConverter,
        player: Player,
        itemStack: ItemStack,
        needed: Int,
    ): Boolean {
        if (needed == 0) return true
        var amount = 0
        for (inventoryItem in player.inventory.contents) {
            if (inventoryItem == null) continue
            if (isSimilar(converter, itemStack, inventoryItem)) {
                amount += inventoryItem.amount
                if (amount >= needed) return true
            }
        }
        return false
    }

    /**
     * Removes [amount] of items matching the same template as [itemStack] from the player's
     * inventory.
     *
     * @return the number of items actually removed
     */
    fun takeItem(
        converter: ItemStackConverter,
        player: Player,
        itemStack: ItemStack,
        amount: Int,
    ): Int {
        var toTake = amount
        var totalTaken = 0
        for (playerItem in player.inventory.contents) {
            if (playerItem == null) continue
            if (isSimilar(converter, itemStack, playerItem)) {
                val takeNext = minOf(toTake, playerItem.amount)
                val toRemove = playerItem.clone()
                toRemove.amount = takeNext
                player.inventory.removeItem(toRemove)
                totalTaken += takeNext
                toTake -= takeNext
                if (toTake <= 0) break
            }
        }
        return totalTaken
    }

    /**
     * Adds an item to the inventory. Overflow items are dropped at the given location.
     *
     * TODO: Replace with smart stacking once InventoryHelper.addItem() is implemented.
     */
    fun addItem(inventory: Inventory, itemStack: ItemStack, dropLocation: Location? = null) {
        val overflow = inventory.addItem(itemStack)
        if (dropLocation != null) {
            for ((_, leftOver) in overflow) {
                dropLocation.world?.dropItem(dropLocation, leftOver)
            }
        }
    }
}
