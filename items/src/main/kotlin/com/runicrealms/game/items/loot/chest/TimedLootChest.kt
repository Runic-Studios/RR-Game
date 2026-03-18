package com.runicrealms.game.items.loot.chest

import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancyholograms.api.hologram.Hologram
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

private val DEFAULT_HOLOGRAM_OFFSET = Vector(0.5, 1.5, 0.5)
private const val DEFAULT_HOLOGRAM_LINE = "<gold>Loot Chest</gold> <gray>-</gray> <yellow>%time%s"

/**
 * A temporary loot chest that appears for a limited duration, with a hologram countdown display.
 * After the duration expires, the chest and hologram are removed.
 *
 * The hologram offset and line format are configurable; defaults match the old system. The `%time%`
 * token in hologram lines is replaced with the remaining time string.
 */
open class TimedLootChest(
    override val templateID: String,
    override val location: Location,
    /** Duration in seconds before this chest despawns. */
    val durationSeconds: Int,
    /** The pre-generated loot items to place in this chest. */
    val lootItems: List<ItemStack>,
    /** Display title shown when the chest inventory is opened. */
    val title: String = "Loot Chest",
    /**
     * Offset from [location] where the hologram is placed. Defaults to (0.5, 1.5, 0.5) matching the
     * old system's behaviour.
     */
    val hologramOffset: Vector? = null,
    /**
     * Hologram line format. Use `%time%` as a placeholder for the remaining time string. Defaults
     * to the old system's format if null.
     */
    val hologramLines: List<String>? = null,
) : LootChest {

    private var hologram: Hologram? = null
    private var remainingSeconds: Int = durationSeconds

    /** Creates and displays the countdown hologram above the chest location. */
    fun createHologram() {
        val hologramName = buildHologramName()
        val offset = hologramOffset ?: DEFAULT_HOLOGRAM_OFFSET
        val hologramLocation = location.clone().add(offset)
        val hologramData = TextHologramData(hologramName, hologramLocation)
        hologramData.isPersistent = false
        hologramData.text = buildHologramLines(remainingSeconds)

        val hologramManager = FancyHologramsPlugin.get().hologramManager
        val createdHologram = hologramManager.create(hologramData)
        hologramManager.addHologram(createdHologram)
        createdHologram.refreshForViewersInWorld()
        hologram = createdHologram
    }

    /** Updates the countdown display. Returns true if the chest should still be active. */
    fun tickCountdown(): Boolean {
        remainingSeconds--
        if (remainingSeconds <= 0) {
            return false
        }
        val currentHologram = hologram ?: return false
        val data = currentHologram.data as? TextHologramData ?: return false
        data.text = buildHologramLines(remainingSeconds)
        currentHologram.refreshForViewersInWorld()
        return true
    }

    /** Removes the hologram and cleans up this timed chest. */
    fun cleanup() {
        val currentHologram = hologram ?: return
        try {
            val hologramManager = FancyHologramsPlugin.get().hologramManager
            hologramManager.removeHologram(currentHologram)
        } catch (exception: Exception) {
            logger.warn("Failed to remove hologram for timed loot chest at $location", exception)
        }
        hologram = null
    }

    /** Returns the remaining seconds on this timed chest. */
    fun getRemainingSeconds(): Int = remainingSeconds

    protected open fun buildHologramName(): String {
        val world = location.world?.name ?: "unknown"
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ
        return "lootchest_timed_${world}_${blockX}_${blockY}_${blockZ}"
    }

    private fun buildHologramLines(seconds: Int): List<String> {
        val timeString = formatTime(seconds)
        val lines = hologramLines ?: listOf(DEFAULT_HOLOGRAM_LINE)
        return lines.map { line -> line.replace("%time%", timeString) }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSecs = seconds % 60
        return if (minutes > 0) "${minutes}m ${remainingSecs}s" else "${remainingSecs}s"
    }
}
