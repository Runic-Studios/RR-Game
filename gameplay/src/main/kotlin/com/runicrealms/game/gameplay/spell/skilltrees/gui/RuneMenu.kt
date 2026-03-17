package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.common.util.breakLines
import com.runicrealms.game.common.util.colorFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.items.DisplayItem
import nl.odalitadevelopments.menus.items.buttons.OpenMenuItem
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The Ancient Runestone inventory: entry point for the skill tree and spell editor system.
 *
 * Layout matches the old RuneGUI.java (45-slot / 5-row chest):
 *   slot 11 (row 1, col 2): Open Skill Trees
 *   slot 13 (row 1, col 4): Open Spell Editor
 *   slot 15 (row 1, col 6): Close
 *   slot 31 (row 3, col 4): Status Effect Key tooltip
 */
@Menu(title = "Ancient Runestone", type = MenuType.CHEST_5_ROW)
class RuneMenu
@AssistedInject
constructor(
    private val spellManager: SpellManager,
    private val subClassMenuFactory: SubClassMenu.Factory,
    private val spellEditorMenuFactory: SpellEditorMenu.Factory,
) : PlayerMenuProvider {

    interface Factory {
        fun create(): RuneMenu
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val playerClass = spellManager.getPlayerClassType(player.uniqueId)

        // slot 11: Skill Trees
        menuContents.set(
            1,
            2,
            OpenMenuItem.of(
                buildSkillTreeButton(playerClass.name.lowercase().replaceFirstChar { it.uppercase() }),
                subClassMenuFactory.create(playerClass),
            ),
        )

        // slot 13: Spell Editor
        menuContents.set(
            1,
            4,
            OpenMenuItem.of(
                buildSpellEditorButton(),
                spellEditorMenuFactory.create(SpellEditorMenu.NO_SPELL, SpellEditorMenu.NO_SLOT),
            ),
        )

        // slot 15: Close
        menuContents.set(
            1,
            6,
            ClickableItem.of(buildCloseButton()) { click ->
                (click.whoClicked as? Player)?.closeInventory()
            },
        )

        // slot 31: Status Effect Key
        menuContents.set(3, 4, DisplayItem.of(buildStatusEffectTooltip()))
    }

    private fun buildSkillTreeButton(className: String): ItemStack {
        val lore =
            "&7Open the skill trees for the &a$className&7 class! " +
                "Earn skill points by leveling-up and spend them on unique and powerful perks!"
        return ItemStack(Material.PAPER).apply {
            editMeta { meta ->
                meta.displayName(Component.text("Open Skill Trees", NamedTextColor.GREEN))
                meta.lore(lore.breakLines().map { it.colorFormat() })
            }
        }
    }

    private fun buildSpellEditorButton(): ItemStack {
        val lore =
            "&7Configure your active spells! Set spells to be executed by different key combos!"
        return ItemStack(Material.NETHER_WART).apply {
            editMeta { meta ->
                meta.displayName(
                    Component.text("Open Spell Editor", NamedTextColor.LIGHT_PURPLE)
                )
                meta.lore(lore.breakLines().map { it.colorFormat() })
            }
        }
    }

    private fun buildCloseButton(): ItemStack =
        ItemStack(Material.BARRIER).apply {
            editMeta { meta ->
                meta.displayName(Component.text("Close", NamedTextColor.RED))
                meta.lore(listOf(Component.text("Close the menu", NamedTextColor.GRAY)))
            }
        }

    private fun buildStatusEffectTooltip(): ItemStack =
        ItemStack(Material.PRISMARINE_CRYSTALS).apply {
            editMeta { meta ->
                // Two-colour display name matching old: RED "[?] " + YELLOW "Status Effect Key"
                meta.displayName(
                    Component.text("[?] ", NamedTextColor.RED)
                        .append(Component.text("Status Effect Key", NamedTextColor.YELLOW))
                )
                meta.lore(
                    buildList {
                        add(Component.empty())
                        for (statusEffect in RunicStatusEffect.entries) {
                            // DARK_AQUA + BOLD for effect name, matching old ChatColor.DARK_AQUA + BOLD
                            add(
                                Component.text(
                                    "${statusEffect.displayName} -",
                                    NamedTextColor.DARK_AQUA,
                                    TextDecoration.BOLD,
                                )
                            )
                            // Description word-wrapped at 28 chars, matching ChatUtils.formattedText
                            addAll(
                                statusEffect.description.breakLines().map {
                                    Component.text(it, NamedTextColor.GRAY)
                                }
                            )
                        }
                    }
                )
            }
        }
}
