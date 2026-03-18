package com.runicrealms.game.items.command

import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.items.config.item.GameItemTemplate
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.game.items.util.ItemInventoryUtil
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class InventoryHelper @Inject constructor(val converter: ItemStackConverter) {

    /**
     * A method used to clear an inventory of all items that match the provided template
     *
     * @param inventory the inventory to wipe
     * @param amount the amount to remove
     * @param template the template
     * @param sender the user who initiated the clear inventory
     * @param ignoreItemStacks if an invalid item is found, if the wipe should be stopped
     */
    fun clearInventory(
        inventory: Inventory,
        amount: Int,
        template: GameItemTemplate?,
        sender: CommandSender?,
        ignoreItemStacks: Boolean,
    ) {
        var amountRemoved = 0
        val contents = inventory.contents
        for (i in contents.indices) {
            if (contents[i] != null && contents[i]!!.type != Material.AIR) {
                if (amount == -1 || amountRemoved < amount) {
                    val item = converter.convertToGameItem(contents[i] ?: continue)
                    if (item == null) {
                        if (ignoreItemStacks) {
                            continue
                        }
                        sender?.sendMessage("&dError removing items!".colorFormat())
                        return
                    }
                    var removeItem = false
                    if (template == null) {
                        removeItem = true
                    } else if (item.template == template) {
                        removeItem = true
                    }
                    if (removeItem) {
                        if (contents[i]!!.amount <= amount - amountRemoved || amount == -1) {
                            amountRemoved += contents[i]!!.amount
                            inventory.setItem(i, ItemStack(Material.AIR))
                        } else {
                            amountRemoved += amount - amountRemoved
                            inventory.getItem(i)!!.amount -= (amount - amountRemoved)
                        }
                    }
                }
            }
        }
    }

    fun clearInventory(
        inventory: Inventory,
        amount: Int,
        template: GameItemTemplate?,
        sender: CommandSender?,
    ) {
        clearInventory(inventory, amount, template, sender, false)
    }

    fun clearInventory(
        inventory: Inventory,
        template: GameItemTemplate?,
        sender: CommandSender?,
        ignoreItemStacks: Boolean,
    ) {
        clearInventory(inventory, -1, template, sender, ignoreItemStacks)
    }

    fun clearInventory(inventory: Inventory, template: GameItemTemplate?, sender: CommandSender?) {
        clearInventory(inventory, -1, template, sender, false)
    }

    /**
     * Adds an ItemStack into an inventory while stacking with existing matching items.
     * Items are matched by templateID (same game item template stacks together).
     *
     * @param inventory the inventory to add to
     * @param itemStack the ItemStack to add
     * @return overflow items that could not fit (empty map if everything fit)
     */
    fun addItem(inventory: Inventory, itemStack: ItemStack?): HashMap<Int, ItemStack> {
        if (itemStack == null || itemStack.type == Material.AIR) return HashMap()

        var amountLeft = itemStack.amount
        val contents = inventory.contents

        // First pass: try to stack with existing items that have the same template
        for (i in contents.indices) {
            if (amountLeft <= 0) break
            val existing = contents[i] ?: continue
            if (existing.type == Material.AIR) continue
            if (existing.amount >= existing.maxStackSize) continue
            if (!ItemInventoryUtil.isSimilar(converter, itemStack, existing)) continue

            val spaceInStack = existing.maxStackSize - existing.amount
            val toAdd = minOf(amountLeft, spaceInStack)
            existing.amount += toAdd
            amountLeft -= toAdd
        }

        // Second pass: place remaining in empty slots via vanilla addItem
        if (amountLeft > 0) {
            val remaining = itemStack.clone()
            remaining.amount = amountLeft
            return HashMap(inventory.addItem(remaining))
        }

        return HashMap()
    }

    /**
     * Adds an ItemStack with smart stacking, dropping overflow at the specified location.
     */
    fun addItem(inventory: Inventory, itemStack: ItemStack?, location: Location) {
        val overflow = addItem(inventory, itemStack)
        for ((_, leftOver) in overflow) {
            location.world?.dropItem(location, leftOver)
        }
    }

    /**
     * Handles shift-click (MOVE_TO_OTHER_INVENTORY) transfers between inventories,
     * using smart stacking. Cancels the vanilla event and handles the transfer manually.
     *
     * @param player the player performing the shift-click
     * @param event the click event (will be cancelled)
     * @param clickedItem the item being shifted
     * @param targetInventory the destination inventory
     * @param loopStart the first slot to consider in the target (inclusive)
     * @param loopEnd the last slot to consider in the target (exclusive)
     */
    fun combineItemStacks(
        player: Player,
        event: InventoryClickEvent,
        clickedItem: ItemStack,
        targetInventory: Inventory,
        loopStart: Int = 0,
        loopEnd: Int = targetInventory.size,
    ) {
        event.isCancelled = true

        var amountLeft = clickedItem.amount
        val contents = targetInventory.contents

        // First pass: try to stack with matching items in the target range
        for (i in loopStart..<loopEnd) {
            if (amountLeft <= 0) break
            val existing = contents[i] ?: continue
            if (existing.type == Material.AIR) continue
            if (existing.amount >= existing.maxStackSize) continue
            if (!ItemInventoryUtil.isSimilar(converter, clickedItem, existing)) continue

            val spaceInStack = existing.maxStackSize - existing.amount
            val toAdd = minOf(amountLeft, spaceInStack)
            existing.amount += toAdd
            targetInventory.setItem(i, existing)
            amountLeft -= toAdd
        }

        // Second pass: place remaining in empty slots in the target range
        for (i in loopStart..<loopEnd) {
            if (amountLeft <= 0) break
            if (contents[i] != null && contents[i]!!.type != Material.AIR) continue

            val toPlace = clickedItem.clone()
            toPlace.amount = minOf(amountLeft, toPlace.maxStackSize)
            targetInventory.setItem(i, toPlace)
            amountLeft -= toPlace.amount
        }

        // Update the source slot
        if (amountLeft <= 0) {
            event.currentItem = null
        } else {
            clickedItem.amount = amountLeft
        }
    }
}
