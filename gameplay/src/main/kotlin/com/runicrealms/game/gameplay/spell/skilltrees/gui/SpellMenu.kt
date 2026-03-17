package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import nl.odalitadevelopments.menus.OdalitaMenus
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Shows all unlocked active spells for the player to select from before assigning them to spell
 * slots in [SpellEditorMenu].
 */
@Menu(title = "Unlocked Spells", type = MenuType.CHEST_6_ROW)
class SpellMenu
@AssistedInject
constructor(
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
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

        val unlockedSpells = mutableListOf<Spell>()
        for ((_, tree) in trees) {
            for (spellName in tree.getUnlockedSpellNames()) {
                val spell = spellManager.getSpell(spellName) ?: continue
                if (!spell.isPassive) unlockedSpells.add(spell)
            }
        }

        for ((index, spell) in unlockedSpells.withIndex()) {
            val row = index / 9
            val col = index % 9
            if (row >= 6) break
            menuContents.set(
                row,
                col,
                ClickableItem.of(buildSpellItem(spell)) {
                    odalitaMenus.openMenu(
                        spellEditorMenuFactory.create(spell.name, slotIndex),
                        player,
                    )
                },
            )
        }

        // Back button
        menuContents.set(
            5,
            0,
            ClickableItem.of(
                ItemStack(Material.ARROW).apply {
                    editMeta { it.displayName(Component.text("Back", NamedTextColor.GRAY)) }
                }
            ) {
                odalitaMenus.openMenu(spellEditorMenuFactory.create(null, null), player)
            },
        )
    }

    private fun buildSpellItem(spell: Spell): ItemStack =
        ItemStack(Material.NETHER_STAR).apply {
            editMeta { meta ->
                meta.displayName(Component.text(spell.name, NamedTextColor.AQUA))
                meta.lore(
                    listOf(
                        Component.text(spell.description, NamedTextColor.GRAY),
                        Component.text("Mana: ${spell.manaCost}", NamedTextColor.BLUE),
                        Component.text("Cooldown: ${spell.cooldown}s", NamedTextColor.YELLOW),
                    )
                )
            }
        }
}
