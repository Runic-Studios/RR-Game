package com.runicrealms.game.common.gui

import org.bukkit.Material
import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.gui.structure.Structure
import xyz.xenondevs.invui.item.builder.ItemBuilder

class InvGuiHelper {

    init {
        // Supplier is not needed here as the Item does not do anything
        Structure.addGlobalIngredient(
            '#',
            ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""),
        )

        // Adding the Markers.CONTENT_LIST_SLOT_HORIZONTAL as a global ingredient is also a good
        // idea
        Structure.addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    }
}
