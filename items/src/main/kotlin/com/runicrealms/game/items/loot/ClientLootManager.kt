package com.runicrealms.game.items.loot

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.loot.chest.LootChestTemplate
import com.runicrealms.game.items.loot.chest.RegenerativeLootChest
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Handles rendering loot chests to clients. Manages chest block placement for
 * regenerative chests and handles player interactions with loot chests.
 */
@Singleton
class ClientLootManager
@Inject
constructor(
    private val plugin: Plugin,
    private val lootManager: LootManager,
) : Listener {

    /**
     * Tracks per-player cooldowns for regenerative chests.
     * Maps player UUID -> chest location key -> timestamp when the chest can be looted again.
     */
    private val playerChestCooldowns = HashMap<UUID, HashMap<String, Long>>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
        logger.info("ClientLootManager initialised")
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return

        if (clickedBlock.type != Material.CHEST) return

        val world = clickedBlock.world.name
        val blockX = clickedBlock.x
        val blockY = clickedBlock.y
        val blockZ = clickedBlock.z

        val chest = lootManager.getRegenerativeLootChestAt(world, blockX, blockY, blockZ) ?: return

        event.isCancelled = true
        handleRegenerativeChestOpen(event.player, chest)
    }

    /**
     * Opens a regenerative loot chest for a player, respecting cooldowns.
     */
    private fun handleRegenerativeChestOpen(
        player: Player,
        chest: RegenerativeLootChest,
    ) {
        val chestKey = chest.locationKey()
        val now = System.currentTimeMillis()
        val cooldowns = playerChestCooldowns.getOrPut(player.uniqueId) { HashMap() }
        val cooldownEnd = cooldowns[chestKey]

        if (cooldownEnd != null && now < cooldownEnd) {
            val remainingSeconds = ((cooldownEnd - now) / 1000) + 1
            player.sendMessage(
                Component.text("This chest is on cooldown. Try again in ${remainingSeconds}s.")
            )
            return
        }

        // Generate loot and open chest inventory
        val lootItems = lootManager.generateLootFromTemplate(chest.templateID)
        val inventory = Bukkit.createInventory(
            null,
            LootChestTemplate.CHEST_SLOTS,
            Component.text(chest.title),
        )

        for (item in lootItems) {
            inventory.addItem(item)
        }

        player.openInventory(inventory)

        // Set cooldown
        cooldowns[chestKey] = now + (chest.respawnTimeSeconds * 1000L)
    }

    /**
     * Clears cooldown data for a player (should be called on quit to prevent memory leaks).
     */
    fun clearPlayerData(playerUUID: UUID) {
        playerChestCooldowns.remove(playerUUID)
    }
}
