package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.trove.client.user.UserCharactersTraits
import java.util.function.Supplier
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import nl.odalitadevelopments.menus.OdalitaMenus
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.contents.action.MenuCloseResult
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.items.buttons.OpenMenuItem
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@Menu(title = "CONFIRM: Delete Character?", type = MenuType.CHEST_1_ROW)
class CharacterDeleteMenu
@AssistedInject
constructor(
    private val characterSelectHelper: CharacterSelectHelper,
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
    @Assisted private val slot: Int,
    @Assisted private val userCharactersTraits: UserCharactersTraits,
) : PlayerMenuProvider {

    interface Factory {
        fun create(slot: Int, userCharactersTraits: UserCharactersTraits): CharacterDeleteMenu
    }

    @Inject private lateinit var characterSelectMenuFactory: CharacterSelectMenu.Factory

    @Volatile private var hasSelected = false

    private suspend fun deleteCharacter(player: Player) {
        // TODO actually implement this with trove
        withContext(plugin.asyncDispatcher) {
            player.sendMessage(
                Component.text(
                    "Deleting characters is not currently supported",
                    Style.style(NamedTextColor.RED),
                )
            )
            delay(2500)
        }
        odalitaMenus.openMenu(characterSelectMenuFactory.create(userCharactersTraits), player)
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        println("hello")
        menuContents.set(
            2,
            OpenMenuItem.of(
                characterSelectHelper.goBackItem,
                characterSelectMenuFactory.create(userCharactersTraits),
            ),
        )
        menuContents.set(
            6,
            ClickableItem.of(characterSelectHelper.confirmDeleteItem) { event ->
                if (hasSelected) return@of
                hasSelected = true
                event.whoClicked.closeInventory()
                plugin.launch { deleteCharacter(event.whoClicked as Player) }
            },
        )
        menuContents
            .events()
            .onClose(
                Supplier<MenuCloseResult> {
                    if (hasSelected) MenuCloseResult.CLOSE else MenuCloseResult.KEEP_OPEN
                }
            )
    }
}
