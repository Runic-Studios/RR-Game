package com.runicrealms.game.items.loot.chest

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/**
 * A timed loot chest linked to a MythicMobs boss kill. Spawns when the associated boss is defeated
 * and despawns after the duration expires.
 *
 * MythicMobs event handling is out of scope and stubbed with TODOs.
 *
 * Config format (`loot/timed-loot/<name>.yml`) for type: boss:
 * ```yaml
 * type: boss
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
 *   title: "Boss Loot"
 *   duration: 300
 * hologram:
 *   location:
 *     world: world
 *     x: 100.5
 *     y: 65.5
 *     z: 200.5
 *   lines:
 *     - "<gold>Boss Loot</gold> <gray>-</gray> <yellow>%time%s"
 * boss:
 *   mm-id: MyBossID
 *   loot-damage-threshold: 0.1
 *   loot-range: 512
 *   location:
 *     world: world
 *     x: 100
 *     y: 64
 *     z: 200
 * ```
 */
class BossTimedLoot(
    templateID: String,
    location: Location,
    durationSeconds: Int,
    lootItems: List<ItemStack>,
    title: String = "Loot Chest",
    hologramOffset: Vector? = null,
    hologramLines: List<String>? = null,
    /** The MythicMobs internal mob ID for the boss that triggered this loot. */
    val bossID: String,
    /** Fraction of the boss's max HP a player must deal to qualify for loot (0.0 = anyone). */
    val lootDamageThreshold: Double = 0.0,
    /** Maximum distance in blocks from the boss at death to qualify for loot. */
    val lootRange: Int = 1024,
    /**
     * Optional location to teleport qualifying players to after receiving loot (e.g. spawn point).
     */
    val completeLocation: Location? = null,
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
        return "lootchest_boss_${bossID}_${world}_${blockX}_${blockY}_${blockZ}"
    }
}
