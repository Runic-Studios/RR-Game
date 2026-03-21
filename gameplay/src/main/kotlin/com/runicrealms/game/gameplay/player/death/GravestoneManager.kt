package com.runicrealms.game.gameplay.player.death

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.RunicDeathEvent
import com.runicrealms.game.items.config.item.GameItemTag
import com.runicrealms.game.items.generator.ItemStackConverter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin

/**
 * Manages all active [Gravestone]s. Responsibilities:
 * - Creating gravestones on death (called by [DeathListener])
 * - Ticking timers and updating hologram text every second
 * - Handling player interaction to open gravestone inventories
 * - Collapsing gravestones when empty, when the player dies again, or on shutdown
 *
 * TODO (ModelEngine 4.0.9 interaction): The old codebase used RunicCommon's ModelInteractEvent. In
 * ME4, the base entity is a [com.ticxo.modelengine.api.entity.Dummy] whose underlying Bukkit entity
 * exposes an entity ID via [Gravestone.baseEntityId]. We currently listen to
 * [PlayerInteractAtEntityEvent] and match by entity ID. If this does not fire for ME4 Dummy
 * entities, replace with the appropriate ME4 interaction event.
 */
@Singleton
class GravestoneManager
@Inject
constructor(private val plugin: Plugin, private val itemStackConverter: ItemStackConverter) :
    Listener {

    val gravestoneMap: ConcurrentHashMap<UUID, Gravestone> = ConcurrentHashMap()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        // Tick every second on the MC main thread so Bukkit API calls are safe
        plugin.launch {
            while (true) {
                delay(1000L)
                tickGravestones()
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Public API (called by DeathListener)
    // -----------------------------------------------------------------------------------------

    fun createGravestone(
        player: Player,
        deathLocation: Location,
        inventory: Inventory,
        victimHasPriority: Boolean,
        prioritySeconds: Int,
        durationSeconds: Int,
    ) {
        val gravestone =
            Gravestone(
                player,
                deathLocation,
                inventory,
                victimHasPriority,
                prioritySeconds,
                durationSeconds,
            )
        gravestoneMap[player.uniqueId] = gravestone
    }

    // -----------------------------------------------------------------------------------------
    // Timer tick
    // -----------------------------------------------------------------------------------------

    private fun tickGravestones() {
        val now = System.currentTimeMillis()
        val iter = gravestoneMap.entries.iterator()
        while (iter.hasNext()) {
            val (_, gravestone) = iter.next()
            val elapsed = ((now - gravestone.startTime) / 1000).toInt()
            val remainingPriority = gravestone.priorityTime - elapsed
            val remainingDuration = gravestone.duration - elapsed

            if (remainingDuration <= 0) {
                iter.remove()
                gravestone.collapse(false)
                continue
            }

            if (remainingPriority <= 0) {
                gravestone.priority = false
            }

            gravestone.updateHologramText(
                if (gravestone.priority) remainingPriority else 0,
                remainingDuration,
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------------------------

    /**
     * Intercepts right-click interaction with a gravestone model's base entity.
     *
     * TODO: ModelEngine 4.0.9 — verify that [PlayerInteractAtEntityEvent] fires for ME4 Dummy base
     *   entities. If it does not, switch to whichever ME4 event covers model right-clicks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (gravestoneMap.isEmpty()) return
        val entityId = event.rightClicked.entityId
        val gravestone = gravestoneMap.values.firstOrNull { it.baseEntityId == entityId } ?: return
        event.isCancelled = true
        attemptToOpenGravestone(event.player, gravestone)
    }

    /** Prevents placing items into gravestone inventories and blocks DUNGEON_ITEM removal. */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (gravestoneMap.isEmpty()) return
        val gravestone =
            gravestoneMap.values.firstOrNull { it.inventory == event.clickedInventory } ?: return

        // Block placing items into the gravestone
        if (event.currentItem == null || event.currentItem!!.type == Material.AIR) {
            event.isCancelled = true
            return
        }

        // Block removing dungeon items
        val gameItem = itemStackConverter.convertToGameItem(event.currentItem!!)
        if (gameItem != null && gameItem.template.tags.contains(GameItemTag.DUNGEON_ITEM)) {
            event.isCancelled = true
        }
    }

    /** Collapses and removes a gravestone when its inventory is emptied. */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (gravestoneMap.isEmpty()) return
        val entry =
            gravestoneMap.entries.firstOrNull { it.value.inventory == event.inventory } ?: return
        if (!event.inventory.isEmpty) return
        gravestoneMap.remove(entry.key)
        entry.value.collapse(false)
    }

    /**
     * When a player dies while already having a gravestone, collapse the old one and drop its items
     * so they are not lost.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onRunicDeath(event: RunicDeathEvent) {
        if (event.isCancelled) return
        val existing = gravestoneMap.remove(event.victim.uniqueId) ?: return
        existing.collapse(true)
    }

    /** Collapse all gravestones on shutdown without dropping items (world cleanup). */
    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin.name != "Game") return
        val entries = gravestoneMap.entries.toList()
        gravestoneMap.clear()
        for ((_, gravestone) in entries) {
            gravestone.collapse(false)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun attemptToOpenGravestone(player: Player, gravestone: Gravestone) {
        if (canOpenGravestone(gravestone.uuid, player, gravestone)) {
            Bukkit.getScheduler()
                .runTask(
                    plugin,
                    Runnable {
                        player.playSound(player.location, Sound.BLOCK_SHULKER_BOX_OPEN, 0.5f, 1.0f)
                        player.openInventory(gravestone.inventory)
                    },
                )
        } else {
            player.playSound(player.location, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f)
            player.sendMessage(
                Component.text(
                    "Only the slain player can loot this gravestone until priority ends!",
                    NamedTextColor.RED,
                )
            )
        }
    }

    private fun canOpenGravestone(
        gravestoneOwnerUuid: UUID,
        whoOpened: Player,
        gravestone: Gravestone,
    ): Boolean {
        // The slain player can always open their own gravestone
        if (whoOpened.uniqueId == gravestoneOwnerUuid) return true

        // If priority has expired, anyone may loot
        if (!gravestone.priority) return true

        // TODO: check party membership when party system is migrated.
        // For now, only the slain player can loot during the priority window.
        return false
    }
}
