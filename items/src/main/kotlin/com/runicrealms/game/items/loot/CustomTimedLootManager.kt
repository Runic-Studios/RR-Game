package com.runicrealms.game.items.loot

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.loot.chest.CustomTimedLoot
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Manages active custom timed loot instances. These are script-driven or event-driven
 * timed loot chests that are not tied to a specific MythicMobs boss.
 */
@Singleton
class CustomTimedLootManager
@Inject
constructor(
    private val plugin: Plugin,
    private val lootManager: LootManager,
) {

    /** Map of custom ID to currently active timed loot instances. */
    private val activeLoot = HashMap<String, MutableList<CustomTimedLoot>>()

    private var tickTaskId: Int = -1

    init {
        startTickTask()
        logger.info("CustomTimedLootManager initialised")
    }

    /**
     * Adds a new custom timed loot instance and creates its hologram.
     */
    fun addCustomLoot(loot: CustomTimedLoot) {
        loot.createHologram()
        activeLoot.getOrPut(loot.customID) { mutableListOf() }.add(loot)
    }

    /**
     * Removes all active loot instances for a given custom ID.
     */
    fun clearCustomLoot(customID: String) {
        val lootList = activeLoot.remove(customID) ?: return
        for (loot in lootList) {
            loot.cleanup()
        }
    }

    /**
     * Returns all active loot instances for a given custom ID.
     */
    fun getActiveLoot(customID: String): List<CustomTimedLoot> {
        return activeLoot[customID] ?: emptyList()
    }

    /**
     * Returns all active custom timed loot instances.
     */
    fun getAllActiveLoot(): Collection<CustomTimedLoot> {
        return activeLoot.values.flatten()
    }

    private fun startTickTask() {
        tickTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val iterator = activeLoot.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val lootIterator = entry.value.iterator()
                while (lootIterator.hasNext()) {
                    val loot = lootIterator.next()
                    if (!loot.tickCountdown()) {
                        loot.cleanup()
                        lootIterator.remove()
                    }
                }
                if (entry.value.isEmpty()) {
                    iterator.remove()
                }
            }
        }, 20L, 20L).taskId
    }
}
