package com.runicrealms.game.items.dynamic.placeholder

import com.google.inject.Inject
import com.runicrealms.game.common.util.TextIcons
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.config.item.LevelRequirementHolder
import com.runicrealms.game.items.dynamic.DynamicItemRegistry
import com.runicrealms.game.items.dynamic.DynamicItemTextPlaceholder
import com.runicrealms.game.items.generator.GameItem
import org.bukkit.inventory.ItemStack

/** Replaces \<level> on items with an X or a checkmark */
class DynamicItemLevelRequirementTextPlaceholder
@Inject
constructor(dynamicItemRegistry: DynamicItemRegistry) : DynamicItemTextPlaceholder("level") {

    init {
        dynamicItemRegistry.registerTextPlaceholder(this)
    }

    override fun generateReplacement(
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String? {
        val template = gameItem.template
        if (template !is LevelRequirementHolder) return null

        return if (viewer.bukkitPlayer.level >= template.level) {
            TextIcons.CHECKMARK_ICON
        } else {
            TextIcons.X_ICON
        }
    }
}
