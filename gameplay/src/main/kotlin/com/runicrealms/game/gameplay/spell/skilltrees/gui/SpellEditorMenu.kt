package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.skilltrees.SpellData
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
 * Spell slot assignment editor. Shows 4 spell slot buttons (HOT_BAR_ONE, LEFT_CLICK, RIGHT_CLICK,
 * SWAP_HANDS) and allows the player to assign a spell to each.
 *
 * The [selectedSpellName] is the spell being bound. If null, shows current bindings. A reset button
 * wipes all slots back to class defaults.
 */
@Menu(title = "Spell Editor", type = MenuType.CHEST_6_ROW)
class SpellEditorMenu
@AssistedInject
constructor(
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
    private val userDataRegistry: UserDataRegistry,
    private val skillTreeManager: SkillTreeManager,
    private val spellManager: SpellManager,
    @Assisted val selectedSpellName: String?,
) : PlayerMenuProvider {

    interface Factory {
        fun create(selectedSpellName: String?): SpellEditorMenu
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val uuid = player.uniqueId
        val spellData = skillTreeManager.getSpellData(uuid) ?: return

        fillBackground(menuContents)

        val slotMetas =
            listOf(
                Triple(1, 1, "Hotbar 1") to 0,
                Triple(1, 3, "Left Click") to 1,
                Triple(1, 5, "Right Click") to 2,
                Triple(1, 7, "Swap Hands") to 3,
            )

        for ((info, slotIndex) in slotMetas) {
            val (row, col, label) = info
            val currentSpellName = spellData.getSpellForSlotIndex(slotIndex) ?: "None"
            menuContents.set(
                row,
                col,
                ClickableItem.of(buildSlotItem(label, currentSpellName)) {
                    if (selectedSpellName != null) {
                        assignSpell(player, spellData, slotIndex, selectedSpellName)
                        odalitaMenus.openMenu(this, player) // refresh
                    }
                },
            )
        }

        // Reset button
        menuContents.set(
            5,
            4,
            ClickableItem.of(
                ItemStack(Material.BARRIER).apply {
                    editMeta {
                        it.displayName(Component.text("Reset All Spells", NamedTextColor.RED))
                    }
                }
            ) {
                resetSpells(player, spellData)
                odalitaMenus.openMenu(this, player)
            },
        )

        // Back button
        menuContents.set(
            5,
            0,
            DisplayItem.of(
                ItemStack(Material.ARROW).apply {
                    editMeta { it.displayName(Component.text("← Back", NamedTextColor.GRAY)) }
                }
            ),
        )
    }

    private fun buildSlotItem(label: String, currentSpellName: String): ItemStack =
        ItemStack(Material.PAPER).apply {
            editMeta { meta ->
                meta.displayName(Component.text(label, NamedTextColor.YELLOW))
                meta.lore(
                    listOf(
                        Component.text("Current: $currentSpellName", NamedTextColor.GRAY),
                        if (selectedSpellName != null)
                            Component.text(
                                "Click to assign: $selectedSpellName",
                                NamedTextColor.GREEN,
                            )
                        else Component.text("Open Spell Menu to assign.", NamedTextColor.DARK_GRAY),
                    )
                )
            }
        }

    private fun assignSpell(
        player: Player,
        spellData: SpellData,
        slotIndex: Int,
        spellName: String,
    ) {
        when (slotIndex) {
            0 -> spellData.spellHotbarOne = spellName
            1 -> spellData.spellLeftClick = spellName
            2 -> spellData.spellRightClick = spellName
            3 -> spellData.spellSwapHands = spellName
        }
        val character = userDataRegistry.getCharacter(player.uniqueId) ?: return
        plugin.launch {
            character.withCharacterData {
                spells.spellOneID = spellData.spellHotbarOne
                spells.spellTwoID = spellData.spellLeftClick
                spells.spellThreeID = spellData.spellRightClick
                spells.spellFourID = spellData.spellSwapHands
            }
        }
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f)
    }

    private fun resetSpells(player: Player, spellData: SpellData) {
        val character = userDataRegistry.getCharacter(player.uniqueId) ?: return
        val classType = character.withSyncCharacterData { traits.classType }
        val defaults = SpellData.defaultForClass(classType)
        spellData.spellHotbarOne = defaults.spellHotbarOne
        spellData.spellLeftClick = defaults.spellLeftClick
        spellData.spellRightClick = defaults.spellRightClick
        spellData.spellSwapHands = defaults.spellSwapHands
        plugin.launch {
            character.withCharacterData {
                spells.spellOneID = defaults.spellHotbarOne
                spells.spellTwoID = defaults.spellLeftClick
                spells.spellThreeID = defaults.spellRightClick
                spells.spellFourID = defaults.spellSwapHands
            }
        }
        player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f)
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
}
