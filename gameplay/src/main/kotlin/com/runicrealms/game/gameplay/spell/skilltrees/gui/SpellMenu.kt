package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.util.toLoreComponents
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.skilltrees.SpellData
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
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
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Shows all unlocked active spells for the player to select and assign to a spell slot.
 *
 * Spell items use [SkillTreeGUI.buildPerkItem]-style formatting (spell name + description +
 * mana/cooldown + "» Click to activate").
 */
@Menu(title = "Unlocked Spells", type = MenuType.CHEST_6_ROW)
class SpellMenu
@AssistedInject
constructor(
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
    private val userDataRegistry: UserDataRegistry,
    private val skillTreeManager: SkillTreeManager,
    private val spellManager: SpellManager,
    private val spellEditorMenuFactory: SpellEditorMenu.Factory,
    @Assisted private val slotIndex: Int,
) : PlayerMenuProvider {

    interface Factory {
        fun create(slotIndex: Int): SpellMenu
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val uuid = player.uniqueId
        val trees = skillTreeManager.getSkillTreeDataMap(uuid) ?: return

        fillBorders(menuContents)

        // Back button at slot 0
        menuContents.set(
            0,
            0,
            ClickableItem.of(buildBackButton()) {
                odalitaMenus.openMenu(
                    spellEditorMenuFactory.create(
                        SpellEditorMenu.NO_SPELL,
                        SpellEditorMenu.NO_SLOT,
                    ),
                    player,
                )
            },
        )

        // Collect spells: default class spell first, then all unlocked active spells from trees
        val spells = mutableListOf<Spell>()

        val character = userDataRegistry.getCharacter(uuid)
        val classType = character?.withSyncCharacterData { traits.classType }
        if (classType != null) {
            val defaultSpellName = SpellData.defaultForClass(classType).spellHotbarOne
            val defaultSpell = spellManager.getSpell(defaultSpellName)
            if (defaultSpell != null && !defaultSpell.isPassive) {
                spells += defaultSpell
            }
        }

        for ((_, tree) in trees) {
            for (spellName in tree.getUnlockedSpellNames()) {
                val spell = spellManager.getSpell(spellName) ?: continue
                if (!spell.isPassive && spells.none { it.name == spell.name }) {
                    spells += spell
                }
            }
        }

        // Place spells in interior slots (non-border), starting from slot 10
        // Border slots: 0-8 (row 0), 9/17 (row 1 edges), 18/26 (row 2), 27/35 (row 3),
        //               36/44 (row 4), 45-53 (row 5)
        val borderSlots =
            setOf(
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                17,
                18,
                26,
                27,
                35,
                36,
                44,
                45,
                46,
                47,
                48,
                49,
                50,
                51,
                52,
                53,
            )

        var spellIndex = 0
        for (slot in 0 until 54) {
            if (slot in borderSlots) continue
            val spell = spells.getOrNull(spellIndex++) ?: break
            menuContents.set(
                slot / 9,
                slot % 9,
                ClickableItem.of(buildSpellItem(spell)) {
                    odalitaMenus.openMenu(
                        spellEditorMenuFactory.create(spell.name, slotIndex),
                        player,
                    )
                },
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

    /**
     * Builds a spell item matching old SkillTreeGUI.buildPerkItem(perk, displayPoints=false,
     * description="» Click to activate") for active spells.
     */
    private fun buildSpellItem(spell: Spell): ItemStack =
        ItemStack(Material.NETHER_WART).apply {
            editMeta { meta ->
                meta.displayName(Component.text(spell.name, NamedTextColor.GREEN))
                val spellType = if (spell.isPassive) "PASSIVE SPELL " else "ACTIVE SPELL "
                val lore =
                    buildList<Component> {
                        addAll("\n&6&l$spellType&7${spell.description}".toLoreComponents())
                        if (!spell.isPassive) {
                            add(Component.empty())
                            add(
                                Component.text("Costs ${spell.manaCost}✸", NamedTextColor.DARK_AQUA)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                            add(
                                Component.text("Cooldown ", NamedTextColor.RED)
                                    .append(
                                        Component.text(
                                            "${spell.cooldown.toInt()}s",
                                            NamedTextColor.YELLOW,
                                        )
                                    )
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                        add(Component.empty())
                        add(
                            Component.text("» Click to activate", NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                    }
                meta.lore(lore)
            }
        }

    /**
     * Fills border slots with BLACK_STAINED_GLASS_PANE, matching old GUIUtil.fillInventoryBorders.
     */
    private fun fillBorders(menuContents: MenuContents) {
        val glass =
            ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(Component.empty()) }
            }
        val borderSlots =
            intArrayOf(
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                17,
                18,
                26,
                27,
                35,
                36,
                44,
                45,
                46,
                47,
                48,
                49,
                50,
                51,
                52,
                53,
            )
        for (slot in borderSlots) {
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(glass.clone()))
        }
    }
}
