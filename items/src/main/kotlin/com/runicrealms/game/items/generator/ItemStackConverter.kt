package com.runicrealms.game.items.generator

import com.runicrealms.trove.generated.api.schema.v1.ItemData
import org.bukkit.inventory.ItemStack

interface ItemStackConverter {

    fun convertToGameItem(itemStack: ItemStack): GameItem?

    fun generateItemData(itemStack: ItemStack): ItemData?
}
