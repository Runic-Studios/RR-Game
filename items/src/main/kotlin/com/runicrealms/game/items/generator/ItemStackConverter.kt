package com.runicrealms.game.items.generator

import com.runicrealms.game.data.model.ItemData
import org.bukkit.inventory.ItemStack

interface ItemStackConverter {

    fun convertToGameItem(itemStack: ItemStack): GameItem?

    /**
     * Reads the NBT data embedded in an ItemStack and returns the deserialised [ItemData]. Returns
     * null if the ItemStack has no game item data.
     */
    fun generateItemData(itemStack: ItemStack): ItemData?
}
