package com.runicrealms.game.items.listeners

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.items.config.item.GameItemClickTrigger
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.dynamic.placeholder.DynamicCustomDataTextPlaceholder
import com.runicrealms.game.items.event.GameItemGenericTriggerEvent
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.game.items.util.ItemInventoryUtil
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Handles gold pouch interactions:
 * - Left-click: empties coins from pouch into inventory
 * - Right-click: fills pouch with coins from inventory
 *
 * Uses the customData approach: the pouch stores its coin count in `ItemData.customData["coins"]`
 * and displays it via a `<coins>` dynamic placeholder.
 */
class GoldPouchListener
@Inject
constructor(
    private val plugin: Plugin,
    private val itemStackConverter: ItemStackConverter,
    private val templateRegistry: GameItemTemplateRegistry,
    private val placeholderFactory: DynamicCustomDataTextPlaceholder.Factory,
) : Listener {

    private val playersUpdatingPouches = HashSet<UUID>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
        placeholderFactory.create(COINS_KEY, COINS_KEY)
    }

    @EventHandler
    fun onGoldPouchTrigger(event: GameItemGenericTriggerEvent) {
        if (playersUpdatingPouches.contains(event.player.uniqueId)) return
        if (event.item.template.id != POUCH_ID) return

        val player = event.player

        // Dupe prevention: ignore if the same item is in the offhand
        if (
            player.inventory.itemInOffHand.type != Material.AIR &&
                ItemInventoryUtil.isSimilar(
                    itemStackConverter,
                    player.inventory.itemInOffHand,
                    event.itemStack,
                )
        )
            return

        playersUpdatingPouches.add(player.uniqueId)

        val currentCoins = event.item.getCustomData(COINS_KEY)?.toIntOrNull() ?: 0
        val maxCoins = event.item.getCustomData(MAX_COINS_KEY)?.toIntOrNull() ?: 0

        if (event.trigger.type == GameItemClickTrigger.Type.LEFT_CLICK) {
            // Empty the pouch: give coins to player, set pouch coins to 0
            player.playSound(player.location, Sound.ENTITY_HORSE_SADDLE, 0.5f, 1.0f)

            // Remove the old pouch
            ItemInventoryUtil.takeItem(itemStackConverter, player, event.itemStack, 1)

            // Give coins in stacks of 64
            if (currentCoins > 0) {
                val coinTemplate = templateRegistry.getItemTemplate(COIN_TEMPLATE_ID) ?: return
                var remaining = currentCoins
                while (remaining > 0) {
                    val stackSize = minOf(remaining, 64)
                    val coinItem = templateRegistry.generateGameItem(coinTemplate)
                    val coinStack = coinItem.generateItemStack(stackSize)
                    ItemInventoryUtil.addItem(player.inventory, coinStack, player.location)
                    remaining -= stackSize
                }
            }

            // Give back the empty pouch
            event.item.setCustomData(COINS_KEY, "0")
            val emptyPouch = event.item.generateItemStack(1)
            ItemInventoryUtil.addItem(player.inventory, emptyPouch, player.location)
        } else if (event.trigger.type == GameItemClickTrigger.Type.RIGHT_CLICK) {
            // Fill the pouch: take coins from inventory, add to pouch
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f)

            val coinTemplate = templateRegistry.getItemTemplate(COIN_TEMPLATE_ID) ?: return
            val coinRefItem = templateRegistry.generateGameItem(coinTemplate).generateItemStack(1)

            val newCoins = fillPouch(currentCoins, maxCoins, player, coinRefItem)

            // Remove old pouch and give back the filled one
            ItemInventoryUtil.takeItem(itemStackConverter, player, event.itemStack, 1)
            event.item.setCustomData(COINS_KEY, newCoins.toString())
            val filledPouch = event.item.generateItemStack(1)
            ItemInventoryUtil.addItem(player.inventory, filledPouch)
        }

        // Cooldown to prevent exploit
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                Runnable { playersUpdatingPouches.remove(player.uniqueId) },
                INTERACT_DELAY_TICKS,
            )
    }

    private fun fillPouch(
        currentAmount: Int,
        maxAmount: Int,
        player: org.bukkit.entity.Player,
        coinRefItem: org.bukkit.inventory.ItemStack,
    ): Int {
        val amountToFill = maxAmount - currentAmount
        if (amountToFill <= 0) return currentAmount

        // Try to fill completely first
        if (ItemInventoryUtil.hasItem(itemStackConverter, player, coinRefItem, amountToFill)) {
            ItemInventoryUtil.takeItem(itemStackConverter, player, coinRefItem, amountToFill)
            return maxAmount
        }

        // Otherwise fill in decreasing stack sizes
        var coins = currentAmount
        for (stackSize in FILL_STACK_SIZES) {
            while (
                coins < maxAmount &&
                    ItemInventoryUtil.hasItem(itemStackConverter, player, coinRefItem, stackSize)
            ) {
                ItemInventoryUtil.takeItem(itemStackConverter, player, coinRefItem, stackSize)
                coins += stackSize
            }
        }
        return coins
    }

    companion object {
        private const val POUCH_ID = "gold-pouch"
        private const val COIN_TEMPLATE_ID = "coin"
        private const val COINS_KEY = "coins"
        private const val MAX_COINS_KEY = "maxCoins"
        private const val INTERACT_DELAY_TICKS = 10L
        private val FILL_STACK_SIZES = intArrayOf(64, 48, 32, 16, 8, 4, 2, 1)
    }
}
