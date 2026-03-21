package com.runicrealms.game.items.loot.chest

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection

/**
 * A persistent loot chest that respawns at a fixed location after being looted. These chests are
 * saved to and loaded from the `loot/regenerative-chests.yml` config file.
 *
 * YAML format per chest section (under `chests.<id>`):
 * ```yaml
 * location:
 *   world: world
 *   x: 100
 *   y: 64
 *   z: 200
 *   direction: NORTH
 * template: my-chest-type
 * regeneration-time: 300
 * min-level: 1
 * item-level:
 *   min: 1
 *   max: 60
 * title: "&6Loot Chest"
 * model: NORMAL         # optional
 * conditions:           # optional
 *   1:
 *     type: item
 *     template-id: my-key
 *     count: 1
 *     take-item: true
 * ```
 */
data class RegenerativeLootChest(
    override val templateID: String,
    override val location: Location,
    /** Time in seconds before the chest respawns after being looted. */
    val respawnTimeSeconds: Int,
    /** Minimum player level required to open this chest (0 = no requirement). */
    val minLevel: Int = 0,
    /** Minimum item level for loot generation. */
    val minItemLevel: Int,
    /** Maximum item level for loot generation. */
    val maxItemLevel: Int,
    /** Display title shown when the chest inventory is opened. */
    val title: String,
    /** Optional model identifier (e.g. "NORMAL", "GOLDEN"). */
    val modelID: String? = null,
    /** The facing direction of the chest block. */
    val direction: BlockFace = BlockFace.NORTH,
    /** Conditions a player must meet to open the chest. */
    val conditions: LootChestConditions = LootChestConditions(),
) : LootChest {

    fun locationKey(): String {
        val world = location.world?.name ?: "unknown"
        return "${world}_${location.blockX}_${location.blockY}_${location.blockZ}"
    }

    fun saveTo(section: ConfigurationSection) {
        section.set("location.world", location.world?.name)
        section.set("location.x", location.blockX)
        section.set("location.y", location.blockY)
        section.set("location.z", location.blockZ)
        section.set("location.direction", direction.name)
        section.set("template", templateID)
        section.set("regeneration-time", respawnTimeSeconds)
        section.set("min-level", minLevel)
        section.set("item-level.min", minItemLevel)
        section.set("item-level.max", maxItemLevel)
        section.set("title", title)
        if (modelID != null) section.set("model", modelID)
        if (conditions.conditions.isNotEmpty()) {
            conditions.addToConfig(section.createSection("conditions"))
        }
    }

    companion object {
        fun loadFrom(section: ConfigurationSection): RegenerativeLootChest? {
            val locationSection = section.getConfigurationSection("location") ?: return null
            val worldName = locationSection.getString("world") ?: return null
            val world = Bukkit.getWorld(worldName) ?: return null
            val posX = locationSection.getInt("x")
            val posY = locationSection.getInt("y")
            val posZ = locationSection.getInt("z")
            val directionName = locationSection.getString("direction", "NORTH") ?: "NORTH"
            val direction =
                runCatching { BlockFace.valueOf(directionName.uppercase()) }
                    .getOrDefault(BlockFace.NORTH)

            val templateID = section.getString("template") ?: return null
            val respawnTime = section.getInt("regeneration-time")
            if (respawnTime == 0) return null
            val minLevel = section.getInt("min-level", 0)
            val minItemLevel = section.getInt("item-level.min").takeIf { it != 0 } ?: return null
            val maxItemLevel = section.getInt("item-level.max").takeIf { it != 0 } ?: return null
            val title = section.getString("title") ?: return null
            val modelID = section.getString("model")

            return RegenerativeLootChest(
                templateID = templateID,
                location = Location(world, posX.toDouble(), posY.toDouble(), posZ.toDouble()),
                respawnTimeSeconds = respawnTime,
                minLevel = minLevel,
                minItemLevel = minItemLevel,
                maxItemLevel = maxItemLevel,
                title = title,
                modelID = modelID,
                direction = direction,
                // Conditions require template registry + converter - loaded separately via
                // LootManager.parseRegenerativeLootChest() after construction
            )
        }
    }
}
