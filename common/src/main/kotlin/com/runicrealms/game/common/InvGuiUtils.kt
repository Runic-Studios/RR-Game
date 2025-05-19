package com.runicrealms.game.common

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem

fun ItemStack.addOnClick(
    handler: Gui.(ClickType, Player, InventoryClickEvent) -> Unit
): ControlItem<Gui> {
    return object : ControlItem<Gui>() {
        override fun getItemProvider(gui: Gui) = ItemWrapper(this@addOnClick)

        override fun handleClick(clicktype: ClickType, player: Player, event: InventoryClickEvent) =
            gui.handler(clicktype, player, event)
    }
}
