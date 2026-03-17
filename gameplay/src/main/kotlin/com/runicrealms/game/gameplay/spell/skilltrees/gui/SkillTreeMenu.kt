package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.StatType
import com.runicrealms.game.common.SubClassType
import com.runicrealms.game.common.util.toLoreComponents
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeData
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreePosition
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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
 * Layout matches old SkillTreeGUI.java (54-slot / 6-row chest):
 *   Row 0 (slots 0-8): border fill (BLACK_STAINED_GLASS_PANE)
 *   slot 0  (row 0, col 0) — Back button (LIGHT_GRAY_STAINED_GLASS_PANE)
 *   slot 4  (row 0, col 4) — Info item (subclass icon + remaining skill points)
 *   [PERK_SLOTS]: 10, 28, 46, 48, 30, 12, 14, 32, 50, 52, 34, 16 — perk icons
 *   Down arrows (RED glass):   slots 19, 23, 37, 41
 *   Up arrows   (GREEN glass): slots 21, 25, 39, 43
 *   Right arrows (BROWN glass): slots 13, 47, 51
 *
 * NOTE: The menu title is static ("Skill Tree") because OdalitaMenus requires a compile-time
 * constant for @Menu(title). The subclass name is shown in the info item at slot 4 instead.
 * See SPELL_MIGRATION.md for details.
 */
@Menu(title = "Skill Tree", type = MenuType.CHEST_6_ROW)
class SkillTreeMenu
@AssistedInject
constructor(
    private val odalitaMenus: OdalitaMenus,
    private val userDataRegistry: UserDataRegistry,
    private val skillTreeManager: SkillTreeManager,
    private val spellManager: SpellManager,
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
        val availablePoints = skillTreeManager.getAvailableSkillPoints(uuid, position.value)

        fillTopRowBorder(menuContents)
        placeArrows(menuContents)
        placeBackButton(player, menuContents)
        menuContents.set(0, 4, DisplayItem.of(buildInfoItem(availablePoints)))

        val perks = tree.perks
        for ((index, perk) in perks.withIndex()) {
            val slot = PERK_SLOTS[index]
            val row = slot / 9
            val col = slot % 9
            menuContents.set(
                row,
                col,
                ClickableItem.of(buildPerkItem(perk, tree, availablePoints)) { _ ->
                    if (!perk.isMaxed() && availablePoints >= perk.cost) {
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

    private fun buildInfoItem(availablePoints: Int): ItemStack =
        subClassType.item.clone().apply {
            editMeta { meta ->
                meta.displayName(
                    Component.text("${subClassType.text} Tree Info", NamedTextColor.GREEN)
                )
                val lore = "&7Remaining Skill Points: &a$availablePoints"
                meta.lore(lore.toLoreComponents())
            }
        }

    private fun buildPerkItem(perk: Perk, tree: SkillTreeData, availablePoints: Int): ItemStack {
        return when (perk) {
            is PerkSpell -> buildPerkSpellItem(perk)
            is PerkBaseStat -> buildPerkBaseStatItem(perk)
            else -> ItemStack(Material.STONE)
        }
    }

    private fun buildPerkSpellItem(perk: PerkSpell): ItemStack {
        val spell = spellManager.getSpell(perk.spellName)
        if (spell == null) {
            return ItemStack(Material.BARRIER).apply {
                editMeta { it.displayName(Component.text("Error: spell not found", NamedTextColor.RED)) }
            }
        }
        val material = if (spell.isPassive) Material.PAPER else Material.NETHER_WART
        return ItemStack(material).apply {
            editMeta { meta ->
                // Display: "SpellName [current/max]" in GREEN, matching old displayPoints=true
                meta.displayName(
                    Component.text(
                        "${spell.name} [${perk.currentlyAllocatedPoints}/${perk.maxAllocatedPoints}]",
                        NamedTextColor.GREEN,
                    )
                )
                val spellType = if (spell.isPassive) "PASSIVE SPELL " else "ACTIVE SPELL "
                val loreText = "\n&6&l$spellType&7${spell.description}"
                val lore = loreText.toLoreComponents().toMutableList()
                if (!spell.isPassive) {
                    lore += Component.empty()
                    lore += Component.text("Costs ${spell.manaCost}✸", NamedTextColor.DARK_AQUA)
                            .decoration(TextDecoration.ITALIC, false)
                    lore += Component.text("Cooldown ", NamedTextColor.RED)
                            .append(Component.text("${spell.cooldown.toInt()}s", NamedTextColor.YELLOW))
                            .decoration(TextDecoration.ITALIC, false)
                }
                lore += Component.empty()
                lore += Component.text("» Click to purchase", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false)
                meta.lore(lore)
            }
        }
    }

    private fun buildPerkBaseStatItem(perk: PerkBaseStat): ItemStack {
        val (statName, statIcon, statDesc) = statInfo(perk.stat)
        return ItemStack(statMaterial(perk.stat)).apply {
            editMeta { meta ->
                // Display: "StatName<icon> [current/max]" in GREEN
                meta.displayName(
                    Component.text(
                        "$statName$statIcon [${perk.currentlyAllocatedPoints}/${perk.maxAllocatedPoints}]",
                        NamedTextColor.GREEN,
                    )
                )
                val loreText =
                    "\n&7Bonus per point: &a+${perk.bonusAmount}\n\n&eCharacter Stat &7$statDesc"
                meta.lore(loreText.toLoreComponents())
            }
        }
    }

    private fun fillTopRowBorder(menuContents: MenuContents) {
        val glass =
            ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(Component.empty()) }
            }
        for (col in 0 until 9) {
            menuContents.set(0, col, DisplayItem.of(glass.clone()))
        }
    }

    private fun placeBackButton(player: Player, menuContents: MenuContents) {
        menuContents.set(
            0,
            0,
            ClickableItem.of(buildBackButton()) {
                val classType =
                    userDataRegistry.getCharacter(player.uniqueId)?.withSyncCharacterData {
                        traits.classType
                    } ?: subClassType.classType
                odalitaMenus.openMenu(subClassMenuFactory.create(classType), player)
            },
        )
    }

    private fun buildBackButton(): ItemStack =
        ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta ->
                meta.displayName(Component.text("Return", NamedTextColor.RED))
                meta.lore("&7Return to the previous menu".toLoreComponents())
            }
        }

    private fun placeArrows(menuContents: MenuContents) {
        val emptyName = Component.empty()

        // Down arrows — RED_STAINED_GLASS_PANE at slots 19, 23, 37, 41
        val downArrow =
            ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(emptyName) }
            }
        for (slot in intArrayOf(19, 23, 37, 41)) {
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(downArrow.clone()))
        }

        // Up arrows — GREEN_STAINED_GLASS_PANE at slots 21, 25, 39, 43
        val upArrow =
            ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(emptyName) }
            }
        for (slot in intArrayOf(21, 25, 39, 43)) {
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(upArrow.clone()))
        }

        // Right arrows — BROWN_STAINED_GLASS_PANE at slots 13, 47, 51
        val rightArrow =
            ItemStack(Material.BROWN_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(emptyName) }
            }
        for (slot in intArrayOf(13, 47, 51)) {
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(rightArrow.clone()))
        }
    }

    companion object {
        /** Perk item slots in the 6-row chest (mirrors old SkillTreeGUI.PERK_SLOTS). */
        val PERK_SLOTS = intArrayOf(10, 28, 46, 48, 30, 12, 14, 32, 50, 52, 34, 16)

        /** Material for a given stat, matching old StatsGUI.getStatMaterial(). */
        fun statMaterial(stat: StatType): Material =
            when (stat) {
                StatType.DEXTERITY -> Material.QUARTZ
                StatType.INTELLIGENCE -> Material.LAPIS_LAZULI
                StatType.STRENGTH -> Material.REDSTONE
                StatType.VITALITY -> Material.DIAMOND
                StatType.WISDOM -> Material.EMERALD
            }

        /**
         * Returns (name, icon, description) for a stat, matching old Stat enum fields.
         * Used to build [PerkBaseStat] lore without referencing the old Items module Stat enum.
         */
        fun statInfo(stat: StatType): Triple<String, String, String> =
            when (stat) {
                StatType.DEXTERITY ->
                    Triple("Dexterity", "✦", "Gain spell haste, reducing your spell cooldowns!")
                StatType.INTELLIGENCE ->
                    Triple(
                        "Intelligence",
                        "ʔ",
                        "Deal more magic damage and gain more mana regen!",
                    )
                StatType.STRENGTH -> Triple("Strength", "⚔", "Deal more physical damage!")
                StatType.VITALITY ->
                    Triple("Vitality", "■", "Gain damage reduction and health regen!")
                StatType.WISDOM ->
                    Triple(
                        "Wisdom",
                        "✸",
                        "Gain more spell healing, shielding, max mana and experience!",
                    )
            }
    }
}
