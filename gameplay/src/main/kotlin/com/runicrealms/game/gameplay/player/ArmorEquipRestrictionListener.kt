package com.runicrealms.game.gameplay.player

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.event.ArmorEquipEvent
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.items.generator.GameItemArmor
import com.runicrealms.game.items.generator.ItemStackConverter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Prevents players from equipping armor that belongs to a different class.
 *
 * Mirrors the old ArmorTypeListener from RunicCore.
 */
@Singleton
class ArmorEquipRestrictionListener
@Inject
constructor(
    plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val itemStackConverter: ItemStackConverter,
) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onArmorEquip(event: ArmorEquipEvent) {
        val newPiece = event.newArmorPiece ?: return // unequip - no restriction
        val gameItem = itemStackConverter.convertToGameItem(newPiece) as? GameItemArmor ?: return
        val armorClass = gameItem.armorTemplate.classType
        if (armorClass == ClassType.ANY) return

        val character = userDataRegistry.getCharacter(event.player.uniqueId) ?: return
        val playerClass = character.withSyncCharacterData { traits.classType }
        if (armorClass == playerClass) return

        event.isCancelled = true
        event.player.sendMessage(Component.text(denialMessage(armorClass), NamedTextColor.RED))
    }

    private fun denialMessage(armorClass: ClassType): String =
        when (armorClass) {
            ClassType.ARCHER -> "Archers can only equip mail armor."
            ClassType.CLERIC -> "Clerics can only equip gilded armor."
            ClassType.MAGE -> "Mages can only equip cloth armor."
            ClassType.ROGUE -> "Rogues can only equip leather armor."
            ClassType.WARRIOR -> "Warriors can only equip plate armor."
            else -> "You cannot equip this armor."
        }
}
