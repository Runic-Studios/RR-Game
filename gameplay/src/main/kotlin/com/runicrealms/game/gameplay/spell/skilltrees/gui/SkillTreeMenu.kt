package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.SubClassType
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeData
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreePosition
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell
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

/**
 * Skill tree perk selection menu. Shows all 12 perks for a specific [SkillTreePosition].
 *
 * Perk slots mirror the old SkillTreeGUI.java: [PERK_SLOTS] at fixed indices in the 6-row chest.
 * Arrow indicators are placed at fixed slots to show tree progression flow.
 */
@Menu(title = "Skill Tree", type = MenuType.CHEST_6_ROW)
class SkillTreeMenu
@AssistedInject
constructor(
    private val odalitaMenus: OdalitaMenus,
    private val userDataRegistry: UserDataRegistry,
    private val skillTreeManager: SkillTreeManager,
    private val subClassMenuFactory: SubClassMenu.Factory,
    @Assisted private val subClassType: SubClassType,
    @Assisted private val position: SkillTreePosition,
) : PlayerMenuProvider {

    interface Factory {
        fun create(subClassType: SubClassType, position: SkillTreePosition): SkillTreeMenu
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val uuid = player.uniqueId
        val treesMap = skillTreeManager.getSkillTreeDataMap(uuid) ?: return
        val tree = treesMap[position] ?: return

        fillBackground(menuContents)
        placeArrows(menuContents)
        placeBackButton(player, menuContents)

        val perks = tree.perks
        for ((index, perk) in perks.withIndex()) {
            val slot = PERK_SLOTS[index]
            val row = slot / 9
            val col = slot % 9
            menuContents.set(
                row,
                col,
                ClickableItem.of(
                    buildPerkItem(
                        perk,
                        tree,
                        skillTreeManager.getAvailableSkillPoints(uuid, position.value),
                    )
                ) { event ->
                    if (
                        !perk.isMaxed() &&
                            skillTreeManager.getAvailableSkillPoints(uuid, position.value) >=
                                perk.cost
                    ) {
                        if (skillTreeManager.attemptToPurchasePerk(uuid, position, perk)) {
                            player.playSound(
                                player.location,
                                Sound.ENTITY_PLAYER_LEVELUP,
                                0.5f,
                                1.2f,
                            )
                            onLoad(player, menuContents) // refresh
                        }
                    }
                },
            )
        }
    }

    private fun buildPerkItem(perk: Perk, tree: SkillTreeData, availablePoints: Int): ItemStack {
        val material =
            when {
                perk.isMaxed() -> Material.EMERALD
                availablePoints >= perk.cost -> Material.GOLD_NUGGET
                else -> Material.COAL
            }
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(
                    when (perk) {
                        is PerkSpell -> Component.text(perk.spellName, NamedTextColor.GREEN)
                        is PerkBaseStat ->
                            Component.text(
                                "+${perk.bonusAmount} ${perk.stat.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                NamedTextColor.AQUA,
                            )
                        else -> Component.text("Perk", NamedTextColor.WHITE)
                    }
                )
                val lore = mutableListOf<Component>()
                lore += Component.text("Cost: ${perk.cost} point(s)", NamedTextColor.YELLOW)
                lore +=
                    Component.text(
                        "Allocated: ${perk.currentlyAllocatedPoints}/${perk.maxAllocatedPoints}",
                        if (perk.isMaxed()) NamedTextColor.GREEN else NamedTextColor.GRAY,
                    )
                if (perk is PerkBaseStat) {
                    lore += Component.text("", NamedTextColor.GRAY)
                    lore += Component.text("(Stat bonus: coming soon)", NamedTextColor.DARK_GRAY)
                }
                meta.lore(lore)
            }
        }
    }

    private fun fillBackground(menuContents: MenuContents) {
        val glass =
            ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(Component.empty()) }
            }
        for (row in 0 until 6) {
            for (col in 0 until 9) {
                menuContents.set(row, col, DisplayItem.of(glass.clone()))
            }
        }
    }

    private fun placeBackButton(player: Player, menuContents: MenuContents) {
        menuContents.set(
            5,
            0,
            ClickableItem.of(
                ItemStack(Material.ARROW).apply {
                    editMeta { it.displayName(Component.text("Back", NamedTextColor.GRAY)) }
                }
            ) {
                val classType =
                    userDataRegistry.getCharacter(player.uniqueId)?.withSyncCharacterData {
                        traits.classType
                    } ?: subClassType.classType
                odalitaMenus.openMenu(subClassMenuFactory.create(classType), player)
            },
        )
    }

    private fun placeArrows(menuContents: MenuContents) {
        // Arrow items at fixed indicator slots (mirrors old SkillTreeGUI layout)
        val arrowSlots = listOf(11, 13, 15, 29, 31, 33)
        val arrow =
            ItemStack(Material.ARROW).apply {
                editMeta { it.displayName(Component.text("→", NamedTextColor.YELLOW)) }
            }
        for (slot in arrowSlots) {
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(arrow.clone()))
        }
    }

    companion object {
        /** Perk item slots in the 6-row chest (mirrors old SkillTreeGUI.PERK_SLOTS). */
        val PERK_SLOTS = intArrayOf(10, 28, 46, 48, 30, 12, 14, 32, 50, 52, 34, 16)
    }
}
