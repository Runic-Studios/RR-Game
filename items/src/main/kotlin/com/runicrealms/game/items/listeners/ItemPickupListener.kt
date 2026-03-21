package com.runicrealms.game.items.listeners

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.command.InventoryHelper
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.game.items.util.ItemDataUpdater
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.plugin.Plugin

/**
 * Custom pickup handler for game items. Cancels vanilla pickup for items that carry CBOR item data
 * and instead uses templateID-based smart stacking via [InventoryHelper]. Sends a ProtocolLib
 * COLLECT packet to animate the pickup on the client side.
 *
 * If the player's inventory is full, the item stays on the ground with the remaining overflow
 * amount rather than being silently lost.
 */
@Singleton
class ItemPickupListener
@Inject
constructor(
    plugin: Plugin,
    private val inventoryHelper: InventoryHelper,
    private val itemStackConverter: ItemStackConverter,
) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPickupItem(event: PlayerAttemptPickupItemEvent) {
        val droppedItem = event.item
        val itemStack = droppedItem.itemStack

        // Only intercept game items (those with CBOR data)
        ItemDataUpdater.readItemData(itemStack) ?: return

        event.isCancelled = true

        val player = event.player
        val initialAmount = itemStack.amount

        val overflow = inventoryHelper.addItem(player.inventory, itemStack)

        val amountPickedUp: Int
        if (overflow.isNotEmpty()) {
            val overflowAmount = overflow.values.sumOf { it.amount }
            if (overflowAmount >= initialAmount) {
                // Inventory is completely full; nothing was picked up
                return
            }
            // Partial pickup: leave the overflow on the ground
            amountPickedUp = initialAmount - overflowAmount
            val remaining = itemStack.clone()
            remaining.amount = overflowAmount
            droppedItem.itemStack = remaining
        } else {
            // All items picked up; remove the ground entity
            amountPickedUp = initialAmount
            droppedItem.remove()
        }

        // Send COLLECT animation packet so the client plays the item-fly-to-player animation
        try {
            val packet = PacketContainer(PacketType.Play.Server.COLLECT)
            packet.integers.write(0, droppedItem.entityId)
            packet.integers.write(1, player.entityId)
            packet.integers.write(2, amountPickedUp)
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
        } catch (exception: Exception) {
            // Non-fatal: animation just won't play
        }

        player.updateInventory()
        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
    }
}
