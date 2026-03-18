package com.runicrealms.game.items.loot.chest

import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancyholograms.api.hologram.Hologram
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * A temporary loot chest that appears for a limited duration, with a hologram countdown display.
 * After the duration expires, the chest and hologram are removed.
 */
open class TimedLootChest(
    override val templateID: String,
    override val location: Location,
    /** Duration in seconds before this chest despawns. */
    val durationSeconds: Int,
    /** The pre-generated loot items to place in this chest. */
    val lootItems: List<ItemStack>,
) : LootChest {

    private var hologram: Hologram? = null
    private var remainingSeconds: Int = durationSeconds

    /**
     * Creates and displays the countdown hologram above the chest location.
     */
    fun createHologram() {
        val hologramName = buildHologramName()
        val hologramLocation = location.clone().add(0.5, 1.5, 0.5)
        val hologramData = TextHologramData(hologramName, hologramLocation)
        hologramData.isPersistent = false
        hologramData.text = listOf(formatCountdownText(remainingSeconds))

        val hologramManager = FancyHologramsPlugin.get().hologramManager
        val createdHologram = hologramManager.create(hologramData)
        hologramManager.addHologram(createdHologram)
        createdHologram.refreshForPlayersInRange()
        hologram = createdHologram
    }

    /**
     * Updates the countdown display. Returns true if the chest should still be active.
     */
    fun tickCountdown(): Boolean {
        remainingSeconds--
        if (remainingSeconds <= 0) {
            return false
        }
        val currentHologram = hologram ?: return false
        currentHologram.data.text = listOf(formatCountdownText(remainingSeconds))
        currentHologram.refreshForPlayersInRange()
        return true
    }

    /**
     * Removes the hologram and cleans up this timed chest.
     */
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

    /**
     * Returns the remaining seconds on this timed chest.
     */
    fun getRemainingSeconds(): Int = remainingSeconds

    protected open fun buildHologramName(): String {
        val world = location.world?.name ?: "unknown"
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ
        return "lootchest_timed_${world}_${blockX}_${blockY}_${blockZ}"
    }

    private fun formatCountdownText(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSecs = seconds % 60
        val timeString = if (minutes > 0) {
            "${minutes}m ${remainingSecs}s"
        } else {
            "${remainingSecs}s"
        }
        return "<gold>Loot Chest</gold> <gray>-</gray> <yellow>$timeString</yellow>"
    }
}
