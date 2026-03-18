package com.runicrealms.game.items.loot.chest

/** Defines a reference to a loot table with a min/max count of items to generate from it. */
data class LootChestTableEntry(val tableID: String, val minCount: Int, val maxCount: Int)

/**
 * Defines a chest template that determines what loot is placed inside a chest. The chest has a
 * fixed inventory size of 27 slots (standard single chest).
 */
data class LootChestTemplate(val identifier: String, val tableEntries: List<LootChestTableEntry>) {
    companion object {
        const val CHEST_SLOTS = 27
    }
}
