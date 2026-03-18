package com.runicrealms.game.items.loot.chest

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/**
 * A timed loot chest with a custom string identifier, used for script-driven or event-driven loot
 * spawns that are not tied to a specific boss.
 *
 * Config format (`loot/timed-loot/<name>.yml`) for type: custom:
 * ```yaml
 * type: custom
 * chest:
 *   location:
 *     world: world
 *     x: 100
 *     y: 64
 *     z: 200
 *   template: my-chest-type
 *   item-level:
 *     min: 1
 *     max: 60
 *   min-level: 0
 *   title: "Custom Loot"
 *   duration: 300
 * hologram:
 *   location:
 *     world: world
 *     x: 100.5
 *     y: 65.5
 *     z: 200.5
 *   lines:
 *     - "<gold>Custom Loot</gold> <gray>-</gray> <yellow>%time%s"
 * custom:
 *   identifier: my-custom-loot
 * ```
 */
class CustomTimedLoot(
    templateID: String,
    location: Location,
    durationSeconds: Int,
    lootItems: List<ItemStack>,
    title: String = "Loot Chest",
    hologramOffset: Vector? = null,
    hologramLines: List<String>? = null,
    /** A custom identifier for this timed loot instance. */
    val customID: String,
    val minLevel: Int = 0,
) :
    TimedLootChest(
        templateID,
        location,
        durationSeconds,
        lootItems,
        title,
        hologramOffset,
        hologramLines,
    ) {

    override fun buildHologramName(): String {
        val world = location.world?.name ?: "unknown"
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ
        return "lootchest_custom_${customID}_${world}_${blockX}_${blockY}_${blockZ}"
    }
}
