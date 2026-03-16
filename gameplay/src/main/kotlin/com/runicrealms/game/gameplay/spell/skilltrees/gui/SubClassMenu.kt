package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.SubClassType
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import nl.odalitadevelopments.menus.OdalitaMenus
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.items.DisplayItem
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Sub-class selection menu. Shows 3 subclass options for the player's current class. Selecting a
 * subclass persists [SubClassType] to [CharacterTraits.subClassType] and reconstructs the
 * [SkillTreeData] perk lists.
 */
@Menu(title = "Choose Your Path", type = MenuType.CHEST_3_ROW)
class SubClassMenu
@AssistedInject
constructor(
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
    private val userDataRegistry: UserDataRegistry,
    private val skillTreeManager: SkillTreeManager,
    @Assisted private val playerClass: ClassType,
) : PlayerMenuProvider {

    interface Factory {
        fun create(playerClass: ClassType): SubClassMenu
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val subClasses: Set<SubClassType> =
            when (playerClass) {
                ClassType.ARCHER -> SubClassType.ARCHER_SUBCLASSES
                ClassType.CLERIC -> SubClassType.CLERIC_SUBCLASSES
                ClassType.MAGE -> SubClassType.MAGE_SUBCLASSES
                ClassType.ROGUE -> SubClassType.ROGUE_SUBCLASSES
                ClassType.WARRIOR -> SubClassType.WARRIOR_SUBCLASSES
                ClassType.ANY -> emptySet()
            }

        val subClassList = subClasses.toList()
        val slots = listOf(2 to 2, 2 to 4, 2 to 6)

        for ((index, subClass) in subClassList.withIndex()) {
            val (row, col) = slots.getOrNull(index) ?: continue
            menuContents.set(
                row,
                col,
                ClickableItem.of(buildSubClassItem(subClass)) { selectSubClass(player, subClass) },
            )
        }

        // Fill empty slots with glass
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                menuContents.set(
                    row,
                    col,
                    DisplayItem.of(
                        ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                            editMeta { it.displayName(Component.empty()) }
                        }
                    ),
                )
            }
        }
        for ((index, subClass) in subClassList.withIndex()) {
            val (row, col) = slots.getOrNull(index) ?: continue
            menuContents.set(
                row,
                col,
                ClickableItem.of(buildSubClassItem(subClass)) { selectSubClass(player, subClass) },
            )
        }
    }

    private fun buildSubClassItem(subClass: SubClassType): ItemStack =
        subClass.item.clone().apply {
            editMeta { meta ->
                meta.displayName(Component.text(subClass.text, NamedTextColor.GOLD))
                meta.lore(
                    subClass.description.split("\n").map { Component.text(it, NamedTextColor.GRAY) }
                )
            }
        }

    private fun selectSubClass(player: Player, subClass: SubClassType) {
        val character = userDataRegistry.getCharacter(player.uniqueId) ?: return
        character.withSyncCharacterData { traits.subClassType = subClass }
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f)
        player.sendMessage(
            Component.text("You have chosen the ")
                .append(Component.text(subClass.text, NamedTextColor.GOLD))
                .append(Component.text(" path!"))
        )
        player.closeInventory()
    }
}
