package com.runicrealms.game.items.loot.chest

import org.bukkit.Location
import org.bukkit.inventory.ItemStack

/**
 * A timed loot chest linked to a MythicMobs boss kill.
 * Spawns when the associated boss is defeated and despawns after the duration expires.
 *
 * MythicMobs event handling is out of scope and stubbed with TODOs.
 */
class BossTimedLoot(
    templateID: String,
    location: Location,
    durationSeconds: Int,
    lootItems: List<ItemStack>,
    /** The MythicMobs internal mob ID for the boss that triggered this loot. */
    val bossID: String,
) : TimedLootChest(templateID, location, durationSeconds, lootItems) {

    override fun buildHologramName(): String {
        val world = location.world?.name ?: "unknown"
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ
        return "lootchest_boss_${bossID}_${world}_${blockX}_${blockY}_${blockZ}"
    }
}
