package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
 * The Ancient Runestone inventory: entry point for the skill tree and spell editor system. Shows
 * buttons for Skill Tree, Spell Editor, and a Close button.
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

        menuContents.set(
            1,
            2,
            OpenMenuItem.of(
                ItemStack(Material.PAPER).apply {
                    editMeta {
                        it.displayName(Component.text("Open Skill Trees", NamedTextColor.GREEN))
                        it.lore(
                            listOf(
                                Component.text(
                                    "Class: ${playerClass.name.lowercase().replaceFirstChar { char -> char.uppercase() }}",
                                    NamedTextColor.GRAY,
                                )
                            )
                        )
                    }
                },
                subClassMenuFactory.create(playerClass),
            ),
        )

        menuContents.set(
            2,
            4,
            OpenMenuItem.of(
                ItemStack(Material.NETHER_WART).apply {
                    editMeta {
                        it.displayName(
                            Component.text("Open Spell Editor", NamedTextColor.LIGHT_PURPLE)
                        )
                    }
                },
                spellEditorMenuFactory.create(null, null),
            ),
        )

        menuContents.set(2, 6, DisplayItem.of(buildStatusEffectTooltip()))

        menuContents.set(
            3,
            4,
            ClickableItem.of(
                ItemStack(Material.BARRIER).apply {
                    editMeta { it.displayName(Component.text("Close", NamedTextColor.RED)) }
                }
            ) { click ->
                (click.whoClicked as? Player)?.closeInventory()
            },
        )
    }

    private fun buildStatusEffectTooltip(): ItemStack =
        ItemStack(Material.PRISMARINE_CRYSTALS).apply {
            editMeta { meta ->
                meta.displayName(Component.text("[?] Status Effect Key", NamedTextColor.YELLOW))
                meta.lore(
                    buildList {
                        add(Component.text("", NamedTextColor.GRAY))
                        for (statusEffect in RunicStatusEffect.entries) {
                            add(
                                Component.text(
                                    "${statusEffect.displayName} -",
                                    NamedTextColor.DARK_AQUA,
                                )
                            )
                            add(Component.text(statusEffect.description, NamedTextColor.GRAY))
                        }
                    }
                )
            }
        }
}
