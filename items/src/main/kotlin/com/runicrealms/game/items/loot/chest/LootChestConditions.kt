package com.runicrealms.game.items.loot.chest

/**
 * Conditions that must be met for a player to access a loot chest.
 */
data class LootChestConditions(
    /** Minimum player level required to open the chest. */
    val minLevel: Int = 0,
    /** Maximum player level allowed to open the chest. */
    val maxLevel: Int = Int.MAX_VALUE,
)
