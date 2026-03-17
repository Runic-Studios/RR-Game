package com.runicrealms.game.gameplay.spell.skilltrees.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.skilltrees.SpellData
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.common.util.toLoreComponents
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
 * Spell slot assignment editor. Shows 4 spell slot buttons and a spell-setup summary item.
 *
 * Layout matches old SpellEditorGUI.java (54-slot / 6-row chest with border fill):
 *   Border slots filled with BLACK_STAINED_GLASS_PANE
 *   slot 0  (row 0, col 0) — Back button (LIGHT_GRAY_STAINED_GLASS_PANE)
 *   slot 4  (row 0, col 4) — "Your Spell Setup" summary (POPPED_CHORUS_FRUIT)
 *   slot 5  (row 0, col 5) — Reset button (MILK_BUCKET)
 *   slot 10 (row 1, col 1) — Slot 1: Hotbar 1
 *   slot 16 (row 1, col 7) — Slot 2: Left-click
 *   slot 37 (row 4, col 1) — Slot 3: Right-click
 *   slot 43 (row 4, col 7) — Slot 4: Swap-hands
 *
 * Use [NO_SPELL] and [NO_SLOT] to open in "browse" mode (no spell/slot pre-selected). Guice does
 * not allow null for [@Assisted] parameters, so we use these sentinels instead.
 *
 * TODO: Key binding letters (slot 1 = "1", slot 4 = "F") are hardcoded. They should come from a
 *   player settings system once one is implemented. See SPELL_MIGRATION.md.
 *
 * TODO: Reset button cost calculation is not implemented; cost shows a placeholder. See
 *   SPELL_MIGRATION.md.
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
    private val runeMenuFactory: RuneMenu.Factory,
    private val spellMenuFactory: SpellMenu.Factory,
    private val spellEditorMenuFactory: Factory,
    @Assisted val selectedSpellName: String,
    @Assisted val selectedSlotIndex: Int,
) : PlayerMenuProvider {

    private val hasSelection: Boolean
        get() = selectedSpellName.isNotEmpty() && selectedSlotIndex >= 0

    interface Factory {
        fun create(selectedSpellName: String, selectedSlotIndex: Int): SpellEditorMenu
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val uuid = player.uniqueId
        val spellData = skillTreeManager.getSpellData(uuid) ?: return
        val playerLevel =
            userDataRegistry.getCharacter(uuid)?.withSyncCharacterData { traits.level } ?: 0

        if (hasSelection) {
            if (isSlotUnlocked(selectedSlotIndex, playerLevel)) {
                assignSpell(player, spellData, selectedSlotIndex, selectedSpellName)
            } else {
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f)
                player.sendMessage(
                    Component.text(
                        "That slot unlocks at level ${requiredLevel(selectedSlotIndex)}.",
                        NamedTextColor.RED,
                    )
                )
            }
            odalitaMenus.openMenu(spellEditorMenuFactory.create(NO_SPELL, NO_SLOT), player)
            return
        }

        fillBorders(menuContents)

        // slot 0 — Back button
        menuContents.set(
            0,
            0,
            ClickableItem.of(buildBackButton()) {
                odalitaMenus.openMenu(runeMenuFactory.create(), player)
            },
        )

        // slot 4 — "Your Spell Setup" summary
        menuContents.set(0, 4, DisplayItem.of(buildSpellSetupItem(uuid, spellData)))

        // slot 5 — Reset button
        menuContents.set(
            0,
            5,
            ClickableItem.of(buildResetButton()) {
                resetSpells(player, spellData)
                odalitaMenus.openMenu(this, player)
            },
        )

        // Spell slot buttons at slots 10, 16, 37, 43
        data class SlotMeta(val slot: Int, val index: Int, val letter: String, val name: String, val nameShort: String)
        val slotMetas =
            listOf(
                SlotMeta(10, 0, "1", "Slot One (Hotbar 1)", "Slot One"),
                SlotMeta(16, 1, "L", "Left-click", "Left-click"),
                SlotMeta(37, 2, "R", "Right-click", "Right-click"),
                SlotMeta(43, 3, "F", "Slot Four (Swap-hands)", "Slot Four"),
            )

        for (meta in slotMetas) {
            val unlocked = isSlotUnlocked(meta.index, playerLevel)
            menuContents.set(
                meta.slot / 9,
                meta.slot % 9,
                ClickableItem.of(buildSpellSlotButton(meta.letter, meta.name, meta.nameShort, meta.index, playerLevel)) {
                    if (!unlocked) {
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f)
                        return@of
                    }
                    odalitaMenus.openMenu(spellMenuFactory.create(meta.index), player)
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

    private fun buildSpellSetupItem(uuid: java.util.UUID, spellData: SpellData): ItemStack =
        ItemStack(Material.POPPED_CHORUS_FRUIT).apply {
            editMeta { meta ->
                meta.displayName(Component.text("Your Spell Setup:", NamedTextColor.LIGHT_PURPLE))
                val lore = buildList<Component> {
                    addAll("&d[1] &7Spell Slot One: &f${spellData.spellHotbarOne}".toLoreComponents())
                    addAll("&d[L] &7Spell Left-click: &f${spellData.spellLeftClick}".toLoreComponents())
                    addAll("&d[R] &7Spell Right-click: &f${spellData.spellRightClick}".toLoreComponents())
                    addAll("&d[F] &7Spell Slot Four: &f${spellData.spellSwapHands}".toLoreComponents())
                    add(Component.empty())
                    addAll("&dYour Passives:".toLoreComponents())
                    val passives = getPassiveNames(uuid)
                    if (passives.isEmpty()) {
                        addAll("&7None".toLoreComponents())
                    } else {
                        for (passive in passives) {
                            addAll("&f- $passive".toLoreComponents())
                        }
                    }
                }
                meta.lore(lore)
            }
        }

    /** Collects the names of all purchased passive spells from every skill tree. */
    private fun getPassiveNames(uuid: java.util.UUID): List<String> {
        val trees = skillTreeManager.getSkillTreeDataMap(uuid) ?: return emptyList()
        return trees.values
            .flatMap { tree -> tree.perks }
            .filterIsInstance<PerkSpell>()
            .filter { perk ->
                perk.isPurchased() && spellManager.getSpell(perk.spellName)?.isPassive == true
            }
            .map { it.spellName }
    }

    private fun buildResetButton(): ItemStack =
        ItemStack(Material.MILK_BUCKET).apply {
            editMeta { meta ->
                meta.displayName(Component.text("Reset Skill Trees", NamedTextColor.LIGHT_PURPLE))
                // TODO: Calculate actual reset cost from player level (see SPELL_MIGRATION.md)
                val lore =
                    "\n&6&lCLICK &7to reset and refund your skill points! " +
                        "Current cost: &aTODO"
                meta.lore(
                    buildList {
                        add(Component.empty())
                        addAll(lore.toLoreComponents())
                    }
                )
            }
        }

    private fun buildSpellSlotButton(
        letter: String,
        name: String,
        nameShort: String,
        slotIndex: Int,
        playerLevel: Int,
    ): ItemStack {
        val unlocked = isSlotUnlocked(slotIndex, playerLevel)
        return if (unlocked) {
            ItemStack(Material.PAPER).apply {
                editMeta { meta ->
                    meta.displayName("&d[$letter] Spell $name".colorFormat())
                    val lore = "&7Configure your active spell for &f$nameShort"
                    meta.lore(lore.toLoreComponents())
                }
            }
        } else {
            ItemStack(Material.BARRIER).apply {
                editMeta { meta ->
                    meta.displayName(Component.text("Spell Slot Locked", NamedTextColor.RED))
                    val lore = "&7Reach level [${requiredLevel(slotIndex)}] to unlock this slot!"
                    meta.lore(lore.toLoreComponents())
                }
            }
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

    /**
     * Fills border slots with BLACK_STAINED_GLASS_PANE, matching old GUIUtil.fillInventoryBorders.
     * Border slots: row 0 (all), row 5 (all), col 0 of rows 1-4, col 8 of rows 1-4.
     */
    private fun fillBorders(menuContents: MenuContents) {
        val glass =
            ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(Component.empty()) }
            }
        val borderSlots =
            intArrayOf(
                0, 1, 2, 3, 4, 5, 6, 7, 8,        // row 0
                9, 17,                              // row 1 edges
                18, 26,                             // row 2 edges
                27, 35,                             // row 3 edges
                36, 44,                             // row 4 edges
                45, 46, 47, 48, 49, 50, 51, 52, 53, // row 5
            )
        for (slot in borderSlots) {
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(glass.clone()))
        }
    }

    private fun isSlotUnlocked(slotIndex: Int, playerLevel: Int): Boolean =
        playerLevel >= requiredLevel(slotIndex)

    private fun requiredLevel(slotIndex: Int): Int =
        when (slotIndex) {
            0 -> 0
            1 -> 10
            2 -> 15
            3 -> 20
            else -> Int.MAX_VALUE
        }

    companion object {
        /** Sentinel for "no spell selected". Used so Guice never receives null. */
        const val NO_SPELL = ""

        /** Sentinel for "no slot selected". Slot indices are 0..3. */
        const val NO_SLOT = -1
    }
}
