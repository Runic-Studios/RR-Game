package com.runicrealms.game.items.loot.chest

import org.bukkit.Location
import org.bukkit.inventory.ItemStack

/**
 * A timed loot chest with a custom string identifier, used for script-driven
 * or event-driven loot spawns that are not tied to a specific boss.
 */
class CustomTimedLoot(
    templateID: String,
    location: Location,
    durationSeconds: Int,
    lootItems: List<ItemStack>,
    /** A custom identifier for this timed loot instance. */
    val customID: String,
) : TimedLootChest(templateID, location, durationSeconds, lootItems) {

    override fun buildHologramName(): String {
        val world = location.world?.name ?: "unknown"
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ
        return "lootchest_custom_${customID}_${world}_${blockX}_${blockY}_${blockZ}"
    }
}
