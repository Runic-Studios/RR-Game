package com.runicrealms.game.items.perk

import com.runicrealms.game.common.event.ArmorEquipEvent
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import com.runicrealms.game.items.dynamic.DynamicItemTextPlaceholder
import com.runicrealms.game.items.generator.GameItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


abstract class DynamicItemPerkTextPlaceholder protected constructor(
    placeholder: String,
    private val characterEquipmentCacheRegistry: CharacterEquipmentCacheRegistry
) :
    DynamicItemTextPlaceholder(placeholder) {
    /**
     * Gets the slot in which an item is currently equipped
     * Returns null if not equipped
     */
    protected fun getEquippedSlot(player: Player, gameItem: GameItem, itemStack: ItemStack): EquippedSlot? {
        val template = gameItem.template
        val cache = characterEquipmentCacheRegistry.cachedPlayerStats[player.uniqueId] ?: return null
        // We haven't loaded yet


        if (ArmorEquipEvent.ArmorType.matchType(itemStack) != null) {
            val helmet = cache.getHelmet()
            val itemHelmet = player.inventory.helmet
            if (helmet != null && itemHelmet != null && helmet.template.id == template.id
                && itemHelmet == itemStack
            ) return EquippedSlot.HELMET

            val chestplate = cache.getChestplate()
            val itemChestplate = player.inventory.chestplate
            if (chestplate != null && itemChestplate != null && chestplate.template.id == template.id
                && itemChestplate == itemStack
            ) return EquippedSlot.CHESTPLATE

            val leggings = cache.getLeggings()
            val itemLeggings = player.inventory.leggings
            if (leggings != null && itemLeggings != null && leggings.template.id == template.id
                && itemLeggings == itemStack
            ) return EquippedSlot.LEGGINGS

            val boots = cache.getBoots()
            val itemBoots = player.inventory.boots
            if (boots != null && itemBoots != null && boots.template.id == template.id
                && itemBoots == itemStack
            ) return EquippedSlot.BOOTS
        }

        val offhand = cache.getOffhand()
        val itemOffhand = player.inventory.itemInOffHand
        if (offhand != null && offhand.template.id == template.id
            && itemOffhand == itemStack
        ) return EquippedSlot.OFFHAND

        val weapon = cache.getWeapon()
        val itemWeapon = player.inventory.itemInMainHand
        if (weapon != null && weapon.template.id == template.id
            && itemWeapon == itemStack
        ) return EquippedSlot.WEAPON

        return null
    }

    protected enum class EquippedSlot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS,
        OFFHAND,
        WEAPON
    }
}