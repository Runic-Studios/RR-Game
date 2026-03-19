package com.runicrealms.game.items.loot

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.loot.chest.BossTimedLoot
import io.lumine.mythic.bukkit.MythicBukkit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Manages active boss timed loot instances. When a MythicMobs boss is defeated, a timed loot chest
 * is spawned at the boss's death location.
 *
 * MythicMobs event integration is currently stubbed with TODOs.
 */
@Singleton
class BossTimedLootManager
@Inject
constructor(private val plugin: Plugin, private val lootManager: LootManager) {

    /** Map of boss ID to currently active timed loot instances. */
    private val activeLoot = HashMap<String, MutableList<BossTimedLoot>>()

    /**
     * Per-boss damage tracking: boss entity UUID -> (player UUID -> total damage dealt).
     * Used by [BossTimedLootDamageListener] to record damage contributions for loot thresholds.
     */
    private val bossDamageMap = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Int>>()

    init {
        startTickTask()
        // TODO: Register MythicMobs death event listener to spawn boss loot chests.
        // When a MythicMobs boss dies:
        // 1. Look up the boss's loot configuration via lootManager.getBossTimedLootConfig(mmID)
        // 2. Generate per-player loot via lootManager.generateLootFromTemplate(templateID)
        // 3. Check lootDamageThreshold and lootRange per qualifying player
        // 4. Create a BossTimedLoot per player and call addBossLoot(bossTimedLoot)
        // 5. Teleport players to completeLocation if set
        logger.info("BossTimedLootManager initialised (MythicMobs integration stubbed)")
    }

    /** Adds a new boss timed loot instance and creates its hologram. */
    fun addBossLoot(loot: BossTimedLoot) {
        loot.createHologram()
        activeLoot.getOrPut(loot.bossID) { mutableListOf() }.add(loot)
    }

    /** Removes all active loot instances for a given boss ID. */
    fun clearBossLoot(bossID: String) {
        val lootList = activeLoot.remove(bossID) ?: return
        for (loot in lootList) {
            loot.cleanup()
        }
    }

    /** Returns all active loot instances for a given boss ID. */
    fun getActiveLoot(bossID: String): List<BossTimedLoot> {
        return activeLoot[bossID] ?: emptyList()
    }

    /**
     * Records [damage] dealt by [player] to [boss].
     *
     * Only accumulates damage for entities that are tracked MythicMobs bosses with a configured
     * loot threshold. Non-boss entities are silently ignored.
     *
     * TODO: Implement loot threshold logic once MythicMobs death event wiring is complete.
     *   Currently this method accumulates damage but the data is not acted upon.
     */
    fun trackBossDamage(player: Player, boss: LivingEntity, damage: Int) {
        val optional = MythicBukkit.inst().mobManager.getActiveMob(boss.uniqueId)
        if (!optional.isPresent) return
        // TODO: Filter to only tracked boss types via lootManager.getBossTimedLootConfig(mobType)
        bossDamageMap
            .getOrPut(boss.uniqueId) { ConcurrentHashMap() }
            .merge(player.uniqueId, damage, Int::plus)
    }

    /** Returns all active boss timed loot instances across all bosses. */
    fun getAllActiveLoot(): Collection<BossTimedLoot> {
        return activeLoot.values.flatten()
    }

    private fun startTickTask() {
        // Runs on the Minecraft main thread every second (1000ms) to tick countdowns.
        plugin.launch {
            while (isActive) {
                delay(1000L)
                tickAllLoot()
            }
        }
    }

    private fun tickAllLoot() {
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
    }
}
