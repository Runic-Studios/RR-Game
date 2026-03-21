package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.SubClassType
import com.runicrealms.game.common.util.toLoreComponents
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreePosition
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
 * Sub-class selection menu. Shows 3 subclass options for the player's current class.
 *
 * Empty slots are filled with black stained-glass panes.
 */
@Menu(title = "Choose Your Path", type = MenuType.CHEST_3_ROW)
class SubClassMenu
@AssistedInject
constructor(
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
    private val userDataRegistry: UserDataRegistry,
    private val skillTreeManager: SkillTreeManager,
    private val skillTreeMenuFactory: SkillTreeMenu.Factory,
    private val runeMenuFactory: RuneMenu.Factory,
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

        val glass =
            ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(Component.empty()) }
            }

        // Fill all slots with glass first, then place items on top
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                menuContents.set(row, col, DisplayItem.of(glass.clone()))
            }
        }

        // Back button at slot 0 (row 0, col 0): matches old GUIUtil.BACK_BUTTON style
        menuContents.set(
            0,
            0,
            ClickableItem.of(buildBackButton()) {
                odalitaMenus.openMenu(runeMenuFactory.create(), player)
            },
        )

        // Subclass items at slots 11, 13, 15 (row 1, cols 2/4/6)
        val subClassList = subClasses.toList()
        val slots = listOf(1 to 2, 1 to 4, 1 to 6)
        for ((index, subClass) in subClassList.withIndex()) {
            val (row, col) = slots.getOrNull(index) ?: continue
            menuContents.set(
                row,
                col,
                ClickableItem.of(buildSubClassItem(subClass)) { selectSubClass(player, subClass) },
            )
        }
    }

    private fun buildBackButton(): ItemStack =
        ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta ->
                meta.displayName(Component.text("Return", NamedTextColor.RED))
                meta.lore("&7Return to the previous menu".toLoreComponents())
            }
        }

    private fun buildSubClassItem(subClass: SubClassType): ItemStack =
        subClass.item.clone().apply {
            editMeta { meta ->
                meta.displayName(Component.text(subClass.text, NamedTextColor.GREEN))
                val title = "&7Open the skill tree for the &a${subClass.text}&7 class!"
                val desc = "&7${subClass.description}"
                meta.lore(
                    buildList {
                        add(Component.empty())
                        addAll(title.toLoreComponents())
                        add(Component.empty())
                        addAll(desc.toLoreComponents())
                    }
                )
            }
        }

    private fun selectSubClass(player: Player, subClass: SubClassType) {
        val character = userDataRegistry.getCharacter(player.uniqueId) ?: return
        character.withSyncCharacterData { traits.subClassType = subClass }
        // Trees are already loaded with their own fixed subclasses on character load: do not
        // reload them here. Just record the active subclass and open the tree for its position.
        val position = SkillTreePosition.fromValue(subClass.position)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f)
        player.sendMessage(
            Component.text("You have chosen the ")
                .append(Component.text(subClass.text, NamedTextColor.GOLD))
                .append(Component.text(" path!"))
        )
        odalitaMenus.openMenu(skillTreeMenuFactory.create(subClass, position), player)
    }
}
