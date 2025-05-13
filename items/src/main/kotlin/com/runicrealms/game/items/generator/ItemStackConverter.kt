package com.runicrealms.game.items.generator

import org.bukkit.inventory.ItemStack

interface ItemStackConverter {

    fun convertToGameItem(itemStack: ItemStack): GameItem?

}