package com.runicrealms.game.items.loot.chest

import org.bukkit.Location

/**
 * Sealed interface representing all types of loot chests in the game.
 */
sealed interface LootChest {

    /** The chest template ID that determines what loot to generate. */
    val templateID: String

    /** The world location of this chest. */
    val location: Location
}
