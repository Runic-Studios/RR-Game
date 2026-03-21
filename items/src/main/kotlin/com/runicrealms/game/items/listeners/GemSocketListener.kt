package com.runicrealms.game.items.listeners

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.common.event.ArmorEquipEvent
import com.runicrealms.game.data.model.ArmorData
import com.runicrealms.game.data.model.GemData
import com.runicrealms.game.items.config.item.GameItemArmorTemplate
import com.runicrealms.game.items.config.item.GameItemGemTemplate
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.game.items.util.GemStatUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.plugin.Plugin

/**
 * Handles gem socketing: when a player drags a gem onto an armor piece, this listener applies the
 * gem bonus to the armor's data and regenerates the item.
 */
class GemSocketListener
@Inject
constructor(
    plugin: Plugin,
    private val itemStackConverter: ItemStackConverter,
    private val templateRegistry: GameItemTemplateRegistry,
) : Listener {

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onGemApply(event: InventoryClickEvent) {
        if (event.isCancelled) return
        // SWAP_WITH_CURSOR for normal slots, NOTHING for armor slots
        if (
            event.action != InventoryAction.SWAP_WITH_CURSOR &&
                event.action != InventoryAction.NOTHING
        )
            return

        val currentItem = event.currentItem
        val cursor = event.cursor
        if (currentItem == null || currentItem.type == Material.AIR) return
        if (cursor.type == Material.AIR) return

        // Read ItemData from both items
        val armorItemData = itemStackConverter.generateItemData(currentItem) ?: return
        val gemItemData = itemStackConverter.generateItemData(cursor) ?: return

        // Check that the target is armor and the cursor is a gem
        val armorTemplate =
            templateRegistry.getItemTemplate(armorItemData.templateID) as? GameItemArmorTemplate
                ?: return
        val gemTemplate =
            templateRegistry.getItemTemplate(gemItemData.templateID) as? GameItemGemTemplate
                ?: return

        if (cursor.amount != 1) {
            event.whoClicked.sendMessage(
                Component.text("You can only apply gems one at a time!", NamedTextColor.RED)
            )
            return
        }

        val armorData = armorItemData.typeData as? ArmorData ?: return
        val gemData = gemItemData.typeData as? GemData ?: return

        // Check gem slot availability
        var gemSlotsUsed = 0
        for (existingGem in armorData.gemBonuses) {
            gemSlotsUsed += GemStatUtil.getGemSlots(existingGem.tier)
        }
        if (
            gemSlotsUsed + GemStatUtil.getGemSlots(gemData.bonus.tier) > armorTemplate.maxGemSlots
        ) {
            event.whoClicked.sendMessage(
                Component.text("This item doesn't have enough free gem slots!", NamedTextColor.RED)
            )
            return
        }

        // Add the gem bonus to armor data and regenerate the item
        val newArmorData = armorData.copy(gemBonuses = armorData.gemBonuses + gemData.bonus)
        val newItemData = armorItemData.copy(typeData = newArmorData)
        val newArmor = templateRegistry.generateGameItem(newItemData)
        val generatedItem = newArmor.generateItemStack(currentItem.amount)

        // If the item is in an armor slot, fire ArmorEquipEvent to update stats
        if (event.slotType == InventoryType.SlotType.ARMOR) {
            val armorType = ArmorEquipEvent.ArmorType.matchType(generatedItem)
            if (armorType != null) {
                val armorEvent =
                    ArmorEquipEvent(
                        event.whoClicked as Player,
                        ArmorEquipEvent.EquipMethod.DRAG,
                        armorType,
                        currentItem,
                        generatedItem,
                    )
                Bukkit.getPluginManager().callEvent(armorEvent)
            }
        }

        val gemDisplayName =
            cursor.itemMeta?.displayName() ?: Component.text("Gem", NamedTextColor.WHITE)

        event.whoClicked.sendMessage(
            Component.text()
                .append(Component.text("Applied ", NamedTextColor.GREEN))
                .append(gemDisplayName)
                .append(Component.text(" to ", NamedTextColor.GREEN))
                .append(armorTemplate.display.name)
                .append(Component.text(".", NamedTextColor.GREEN))
                .build()
        )

        event.currentItem = generatedItem
        event.whoClicked.setItemOnCursor(null)
        event.isCancelled = true
    }
}
