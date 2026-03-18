package com.runicrealms.game.items.loot.chest

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection

/**
 * A persistent loot chest that respawns at a fixed location after being looted.
 * These chests are saved to and loaded from YAML configuration.
 */
data class RegenerativeLootChest(
    override val templateID: String,
    override val location: Location,
    /** Time in seconds before the chest respawns after being looted. */
    val respawnTimeSeconds: Int,
    /** Minimum item level for loot generation. */
    val minItemLevel: Int,
    /** Maximum item level for loot generation. */
    val maxItemLevel: Int,
    /** Display title shown when the chest inventory is opened. */
    val title: String,
    /** Optional custom model ID for the chest block. */
    val modelID: String? = null,
) : LootChest {

    /**
     * Generates a unique key for this chest based on its location.
     */
    fun locationKey(): String {
        val world = location.world?.name ?: "unknown"
        return "${world}_${location.blockX}_${location.blockY}_${location.blockZ}"
    }

    /**
     * Serialises this chest to a YAML configuration section.
     */
    fun saveTo(section: ConfigurationSection) {
        section.set("template-id", templateID)
        section.set("world", location.world?.name)
        section.set("x", location.blockX)
        section.set("y", location.blockY)
        section.set("z", location.blockZ)
        section.set("respawn-time", respawnTimeSeconds)
        section.set("min-item-level", minItemLevel)
        section.set("max-item-level", maxItemLevel)
        section.set("title", title)
        if (modelID != null) {
            section.set("model-id", modelID)
        }
    }

    companion object {
        /**
         * Deserialises a regenerative loot chest from a YAML configuration section.
         */
        fun loadFrom(section: ConfigurationSection): RegenerativeLootChest? {
            val templateID = section.getString("template-id") ?: return null
            val worldName = section.getString("world") ?: return null
            val world = Bukkit.getWorld(worldName) ?: return null
            val posX = section.getInt("x")
            val posY = section.getInt("y")
            val posZ = section.getInt("z")
            val respawnTime = section.getInt("respawn-time", 300)
            val minItemLevel = section.getInt("min-item-level", 1)
            val maxItemLevel = section.getInt("max-item-level", 60)
            val title = section.getString("title") ?: "Loot Chest"
            val modelID = section.getString("model-id")

            return RegenerativeLootChest(
                templateID = templateID,
                location = Location(world, posX.toDouble(), posY.toDouble(), posZ.toDouble()),
                respawnTimeSeconds = respawnTime,
                minItemLevel = minItemLevel,
                maxItemLevel = maxItemLevel,
                title = title,
                modelID = modelID,
            )
        }
    }
}
