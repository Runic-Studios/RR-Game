package com.runicrealms.game.gameplay.player.inventory

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.trove.generated.api.schema.v1.ItemDataStack
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

class PlayerInventoryManager
@Inject
constructor(
    plugin: Plugin,
    private val templateRegistry: GameItemTemplateRegistry,
    private val itemStackConverter: ItemStackConverter,
) : Listener {

    private val logger = LoggerFactory.getLogger("gameplay")

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    fun onCharacterJoin(event: GameCharacterLoadEvent) {
        try {
            val items = event.character.withSyncCharacterData { inventory.data.itemsMap }
            for ((slot, itemDataStack) in items) {
                val item = templateRegistry.generateGameItem(itemDataStack.data)
                event.character.bukkitPlayer.inventory.setItem(
                    slot,
                    item.generateItemStack(itemDataStack.count),
                )
            }
        } catch (exception: Exception) {
            event.fail(
                IllegalStateException(
                    "Failed to load inventory for player ${event.character.bukkitPlayer.name} ${event.character.bukkitPlayer.uniqueId}",
                    exception,
                )
            )
        }
    }

    @EventHandler
    fun onCharacterQuit(event: GameCharacterQuitEvent) {
        val items = HashMap<Int, ItemDataStack>()
        var i = 0
        for (item in event.character.bukkitPlayer.inventory.contents) {
            if (item != null) {
                val itemData = itemStackConverter.generateItemData(item)
                if (itemData != null) {
                    items[i++] =
                        ItemDataStack.newBuilder().setData(itemData).setCount(item.amount).build()
                }
            }
            i++
        }
        event.character.withSyncCharacterData {
            inventory.data.clearItems()
            inventory.data.putAllItems(items)
            stageChanges(inventory)
        }
    }
}
