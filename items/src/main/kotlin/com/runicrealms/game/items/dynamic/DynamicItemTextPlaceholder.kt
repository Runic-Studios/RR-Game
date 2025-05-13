package com.runicrealms.game.items.dynamic

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.generator.GameItem
import org.bukkit.inventory.ItemStack

abstract class DynamicItemTextPlaceholder
protected constructor( // Placeholder without the <>
    val identifier: String
) {
    /**
     * Generates the text replacement for this placeholder for a given player WARNING: you should
     * not be converting this item stack into a RunicItem! Any complex operations performed in this
     * function will be repeated potentially hundreds of time per second or even per tick. Be as
     * efficient with replacements as you can, caching values if needed.
     *
     * Null indicates no replacement
     */
    abstract fun generateReplacement(
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String?
}
