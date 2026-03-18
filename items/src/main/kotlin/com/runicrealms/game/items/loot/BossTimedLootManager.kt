package com.runicrealms.game.items.loot

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.loot.chest.BossTimedLoot
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Manages active boss timed loot instances. When a MythicMobs boss is defeated,
 * a timed loot chest is spawned at the boss's death location.
 *
 * MythicMobs event integration is currently stubbed with TODOs.
 */
@Singleton
class BossTimedLootManager
@Inject
constructor(
    private val plugin: Plugin,
    private val lootManager: LootManager,
) {

    /** Map of boss ID to currently active timed loot instances. */
    private val activeLoot = HashMap<String, MutableList<BossTimedLoot>>()

    private var tickTaskId: Int = -1

    init {
        startTickTask()
        // TODO: Register MythicMobs death event listener to spawn boss loot chests.
        // When a MythicMobs boss dies:
        // 1. Look up the boss's loot configuration (template ID, duration)
        // 2. Generate loot via lootManager.generateLootFromTemplate(templateID)
        // 3. Create a BossTimedLoot instance at the boss death location
        // 4. Call addBossLoot(bossTimedLoot)
        logger.info("BossTimedLootManager initialised (MythicMobs integration stubbed)")
    }

    /**
     * Adds a new boss timed loot instance and creates its hologram.
     */
    fun addBossLoot(loot: BossTimedLoot) {
        loot.createHologram()
        activeLoot.getOrPut(loot.bossID) { mutableListOf() }.add(loot)
    }

    /**
     * Removes all active loot instances for a given boss ID.
     */
    fun clearBossLoot(bossID: String) {
        val lootList = activeLoot.remove(bossID) ?: return
        for (loot in lootList) {
            loot.cleanup()
        }
    }

    /**
     * Returns all active loot instances for a given boss ID.
     */
    fun getActiveLoot(bossID: String): List<BossTimedLoot> {
        return activeLoot[bossID] ?: emptyList()
    }

    /**
     * Returns all active boss timed loot instances across all bosses.
     */
    fun getAllActiveLoot(): Collection<BossTimedLoot> {
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
