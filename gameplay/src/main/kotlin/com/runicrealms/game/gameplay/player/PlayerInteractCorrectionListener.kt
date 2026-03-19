package com.runicrealms.game.gameplay.player

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.UUID
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Fixes a Spigot 1.19+ bug where [PlayerInteractEvent] is not fired when the player's crosshair
 * is within 3-5 blocks of a mob entity.
 *
 * Reference: https://www.spigotmc.org/threads/574671/
 *
 * Tracks recent [PlayerInteractEvent] timestamps per player. On an ARM_SWING animation, if no
 * interact event was recorded within the past 10ms, a synthetic LEFT_CLICK_AIR interact event
 * is fired with a 2-tick delay (1 tick is too early for damage processing; 3 ticks is noticeable).
 */
@Singleton
class PlayerInteractCorrectionListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    private val playerInteractions = HashMap<UUID, Long>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerAnimation(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) return

        val current = System.currentTimeMillis()

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val time = playerInteractions.remove(event.player.uniqueId)

            // If an interact event was recorded recently (within 10ms), no correction is needed
            if (time != null && time + 10 > current) return@Runnable

            val item: ItemStack? =
                if (event.player.inventory.itemInMainHand.type != Material.AIR) {
                    event.player.inventory.itemInMainHand
                } else {
                    null
                }

            val interactEvent = PlayerInteractEvent(
                event.player,
                Action.LEFT_CLICK_AIR,
                item,
                null,
                event.player.facing,
            )
            plugin.server.pluginManager.callEvent(interactEvent)
        }, 2L)
    }

    /** Records the timestamp of a real interact event so the correction is suppressed. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        playerInteractions[event.player.uniqueId] = System.currentTimeMillis()
    }

    /** Records item drop as an interact-equivalent to suppress unnecessary correction events. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        playerInteractions[event.player.uniqueId] = System.currentTimeMillis()
    }

    /** Records entity damage as an interact-equivalent to suppress unnecessary correction events. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        playerInteractions[player.uniqueId] = System.currentTimeMillis()
    }

    /** Cleans up state when a player disconnects. */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        playerInteractions.remove(event.player.uniqueId)
    }
}
