package com.runicrealms.game.items

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.config.item.GameItemClickTrigger
import com.runicrealms.game.items.event.GameItemGenericTriggerEvent
import com.runicrealms.game.items.generator.GameItemGeneric
import com.runicrealms.game.items.generator.ItemStackConverter
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin

@Singleton
class GameItemInteractListener
@Inject
constructor(plugin: Plugin, private val itemStackConverter: ItemStackConverter) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val action = event.action
        if (
            action != Action.LEFT_CLICK_AIR &&
                action != Action.LEFT_CLICK_BLOCK &&
                action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK
        )
            return
        val itemStack = event.player.inventory.itemInMainHand
        if (itemStack.type == Material.AIR) return
        val gameItem = itemStackConverter.convertToGameItem(itemStack) as? GameItemGeneric ?: return
        val triggerType =
            GameItemClickTrigger.Type.getFromInteractAction(action, event.player) ?: return
        val matchingTrigger =
            gameItem.genericTemplate.triggers.firstOrNull { it.type == triggerType } ?: return
        Bukkit.getPluginManager()
            .callEvent(
                GameItemGenericTriggerEvent(event.player, gameItem, itemStack, matchingTrigger)
            )
    }
}
