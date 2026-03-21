package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.ItemStackConverter
import nl.odalitadevelopments.menus.OdalitaMenus
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

@Singleton
class RuneListener
@Inject
constructor(
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
    private val runeMenuFactory: RuneMenu.Factory,
    private val itemTemplateRegistry: GameItemTemplateRegistry,
    private val itemStackConverter: ItemStackConverter,
) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onCharacterLoad(event: GameCharacterLoadEvent) {
        val player = event.character.bukkitPlayer
        val slotItem = player.inventory.getItem(RUNE_SLOT)
        if (isRuneItem(slotItem)) return

        val runeTemplate = itemTemplateRegistry.getItemTemplate(RUNE_TEMPLATE_ID) ?: return
        val rune = itemTemplateRegistry.generateGameItem(runeTemplate).generateItemStack(1)
        player.inventory.setItem(RUNE_SLOT, rune)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (event.clickedInventory == player.inventory && event.slot == RUNE_SLOT) {
            event.isCancelled = true
            return
        }
        if (event.hotbarButton == RUNE_SLOT) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK)
            return

        val player = event.player
        if (player.inventory.heldItemSlot != RUNE_SLOT) return
        val itemInHand = player.inventory.getItem(RUNE_SLOT) ?: return
        if (!isRuneItem(itemInHand)) return

        event.isCancelled = true
        odalitaMenus.openMenu(runeMenuFactory.create(), player)
    }

    private fun isRuneItem(itemStack: ItemStack?): Boolean {
        if (itemStack == null || itemStack.type == Material.AIR) return false
        val itemData = itemStackConverter.generateItemData(itemStack) ?: return false
        return itemData.templateID == RUNE_TEMPLATE_ID
    }

    private companion object {
        const val RUNE_TEMPLATE_ID = "rune"
        const val RUNE_SLOT = 0
    }
}
