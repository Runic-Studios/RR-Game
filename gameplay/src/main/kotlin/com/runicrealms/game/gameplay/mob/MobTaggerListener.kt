package com.runicrealms.game.gameplay.mob

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import io.lumine.mythic.bukkit.MythicBukkit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

private const val PRIORITY_DURATION_SECONDS = 10
private const val TAG_TIME_SECONDS = 10

/**
 * Tracks which player first damages a mob and grants them loot priority for a short window.
 *
 * When a player damages a mob, it is "tagged" to that player. When the mob drops loot, the
 * calling system (typically a death handler) should call [dropTaggedLoot] to mark those items
 * as priority for the tagger. Other players cannot pick up priority items for [PRIORITY_DURATION_SECONDS]
 * seconds.
 *
 * Boss-faction mobs (tracked by BossTimedLootDamageListener) are excluded from regular tagging.
 *
 * TODO: Connect [dropTaggedLoot] to the mob death/loot-drop handler once that system is migrated.
 */
@Singleton
class MobTaggerListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    /** Maps player UUID -> mob UUID (the mob that player has tagged). */
    private val taggedMobs = ConcurrentHashMap<UUID, UUID>()

    /** Maps player UUID -> timestamp when the tag was last refreshed. */
    private val taggedTimers = ConcurrentHashMap<UUID, Long>()

    /** Maps item stack reference -> set of UUIDs allowed to pick it up. */
    private val priorityItems = ConcurrentHashMap<ItemStack, MutableSet<UUID>>()

    /** Maps item stack reference -> timestamp when priority was granted. */
    private val priorityTimers = ConcurrentHashMap<ItemStack, Long>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        startCleanupTask()
    }

    private fun startCleanupTask() {
        // Run cleanup asynchronously every second to avoid stalling the main thread.
        plugin.launch {
            while (isActive) {
                delay(1000L)
                removeTags()
            }
        }
    }

    /**
     * Drops [itemStack] at [location] with pickup priority for [player].
     * Other players cannot pick up this item for [PRIORITY_DURATION_SECONDS] seconds.
     */
    fun dropTaggedLoot(player: Player, location: Location, itemStack: ItemStack) {
        priorityItems.getOrPut(itemStack) { ConcurrentHashMap.newKeySet() }.add(player.uniqueId)
        priorityTimers[itemStack] = System.currentTimeMillis()
        player.world.dropItem(location, itemStack)
    }

    /** Returns the player who has priority on loot from [mobId], or null if none. */
    fun getTagger(mobId: UUID): Player? {
        for ((playerId, taggedMob) in taggedMobs) {
            if (taggedMob == mobId) return plugin.server.getPlayer(playerId)
        }
        return null
    }

    /** Returns true if [mobId] is currently tagged by any player. */
    fun isTagged(mobId: UUID): Boolean = taggedMobs.containsValue(mobId)

    /** Prevents non-priority players from picking up priority-tagged items. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val itemStack = event.item.itemStack
        val allowed = priorityItems[itemStack] ?: return
        if (!allowed.contains(event.entity.uniqueId)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        tagMob(event.caster, event.victim)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onMagicDamage(event: MagicDamageEvent) {
        tagMob(event.caster, event.victim)
    }

    private fun removeTags() {
        val now = System.currentTimeMillis()
        taggedTimers.entries.removeIf { (playerId, startTime) ->
            if (now - startTime >= TAG_TIME_SECONDS * 1000L) {
                taggedMobs.remove(playerId)
                true
            } else {
                false
            }
        }
        priorityTimers.entries.removeIf { (itemStack, startTime) ->
            if (now - startTime >= PRIORITY_DURATION_SECONDS * 1000L) {
                priorityItems.remove(itemStack)
                true
            } else {
                false
            }
        }
    }

    private fun tagMob(player: Player, entity: Entity) {
        if (entity is Player) return
        val playerId = player.uniqueId
        val entityId = entity.uniqueId

        // Boss-faction mobs are handled separately by BossTimedLootDamageListener
        val mythicMob = MythicBukkit.inst().mobManager.getActiveMob(entityId)
        if (mythicMob.isPresent) {
            // TODO: Check if this mob is a tracked boss via BossTimedLootManager and skip if so
            //   if (bossTimedLootManager.isBoss(mythicMob.get().mobType)) return
        }

        if (taggedMobs.containsKey(playerId)) {
            // If this player has already tagged the same mob, refresh the timer
            if (taggedMobs[playerId] == entityId) {
                taggedTimers[playerId] = System.currentTimeMillis()
            }
        } else if (!isTagged(entityId)) {
            // Mob not yet tagged by anyone - this player gets priority
            taggedMobs[playerId] = entityId
            taggedTimers[playerId] = System.currentTimeMillis()
        }
    }
}
