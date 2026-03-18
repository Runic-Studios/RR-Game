package com.runicrealms.game.items.weaponskin.ui

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.items.weaponskin.WeaponSkin
import com.runicrealms.game.items.weaponskin.WeaponSkinManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.items.DisplayItem
import nl.odalitadevelopments.menus.items.buttons.PageItem
import nl.odalitadevelopments.menus.iterators.MenuIteratorType
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Paginated weapon skin selection UI built with OdalitaMenus.
 *
 * Layout (6 rows):
 * - Row 0: border (glass panes)
 * - Rows 1-4: skin items (28 interior slots)
 * - Row 5: border with previous (col 0) and next (col 8) page navigation
 *
 * Left-click: apply skin to all matching weapons in player's inventory Right-click: remove skin
 * from all matching weapons in player's inventory
 *
 * Ineligible skins (permission not met) are shown as locked grey glass panes.
 */
@Menu(title = "Weapon Appearances", type = MenuType.CHEST_6_ROW)
class WeaponAppearancesMenu
@AssistedInject
constructor(
    private val weaponSkinManager: WeaponSkinManager,
    @Assisted private val availableSkins: List<WeaponSkin>,
) : PlayerMenuProvider {

    interface Factory {
        fun create(availableSkins: List<WeaponSkin>): WeaponAppearancesMenu
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        fillBorders(menuContents)

        val iterator =
            menuContents.createIterator("weapon_skins_iter", MenuIteratorType.HORIZONTAL, 1, 0)

        val pagination =
            menuContents
                .pagination("weapon_skins", ITEMS_PER_PAGE, iterator)
                .items(
                    availableSkins.map { skin ->
                        java.util.function.Supplier { buildSkinMenuItem(player, skin) }
                    }
                )
                .create()

        menuContents.setPageSwitchUpdateItem(5, 0) {
            PageItem.previous(pagination, buildNavItem("<- Previous Page"))
        }
        menuContents.setPageSwitchUpdateItem(5, 8) {
            PageItem.next(pagination, buildNavItem("Next Page ->"))
        }
    }

    private fun buildSkinMenuItem(
        player: Player,
        skin: WeaponSkin,
    ): nl.odalitadevelopments.menus.items.MenuItem {
        val eligible = weaponSkinManager.canActivateSkin(player, skin)
        if (!eligible) {
            val lockedItem =
                ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
                    editMeta { meta ->
                        meta.displayName(
                            Component.text(skin.name ?: skin.id, NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                        meta.lore(
                            listOf(
                                Component.text("Locked", NamedTextColor.RED)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        )
                    }
                }
            return DisplayItem.of(lockedItem)
        }

        val displayItem =
            ItemStack(skin.material).apply {
                editMeta { meta ->
                    meta.displayName(
                        Component.text(skin.name ?: skin.id, NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    meta.lore(
                        listOf(
                            Component.text("Left-click to apply", NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false),
                            Component.text("Right-click to remove", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false),
                        )
                    )
                }
            }

        return ClickableItem.of(displayItem) { event ->
            val clicker = event.whoClicked as? Player ?: return@of
            if (event.isLeftClick) {
                weaponSkinManager.activateSkin(clicker, skin)
                clicker.sendMessage(
                    Component.text("Applied skin '${skin.name ?: skin.id}'.", NamedTextColor.GREEN)
                )
            } else if (event.isRightClick) {
                weaponSkinManager.deactivateSkin(clicker, skin)
                clicker.sendMessage(
                    Component.text("Removed skin '${skin.name ?: skin.id}'.", NamedTextColor.GRAY)
                )
            }
        }
    }

    private fun buildNavItem(label: String): ItemStack =
        ItemStack(Material.ARROW).apply {
            editMeta { meta ->
                meta.displayName(
                    Component.text(label, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
        }

    private fun fillBorders(menuContents: MenuContents) {
        val glass =
            ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                editMeta { it.displayName(Component.empty()) }
            }
        // Row 0 (top border), edges of rows 1-4, row 5 (bottom border)
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

    companion object {
        private const val ITEMS_PER_PAGE = 28
    }
}
