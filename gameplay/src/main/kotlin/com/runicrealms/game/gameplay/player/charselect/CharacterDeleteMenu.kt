package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.model.CharacterTraits
import com.runicrealms.game.data.repository.PlayerRepository
import java.util.function.Supplier
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

@Menu(title = "CONFIRM: Delete Character?", type = MenuType.CHEST_1_ROW)
class CharacterDeleteMenu
@AssistedInject
constructor(
    private val characterSelectHelper: CharacterSelectHelper,
    private val plugin: Plugin,
    private val odalitaMenus: OdalitaMenus,
    private val userDataRegistry: UserDataRegistry,
    private val playerRepository: PlayerRepository,
    @Assisted private val slot: Int,
    @Assisted private val userCharactersTraits: Map<Int, CharacterTraits>,
) : PlayerMenuProvider {

    interface Factory {
        fun create(slot: Int, userCharactersTraits: Map<Int, CharacterTraits>): CharacterDeleteMenu
    }

    @Inject private lateinit var characterSelectMenuFactory: CharacterSelectMenu.Factory

    @Volatile private var hasSelected = false

    private suspend fun deleteCharacter(player: Player) {
        val gamePlayer = userDataRegistry.getPlayer(player.uniqueId)
        if (gamePlayer == null) {
            player.sendMessage(
                Component.text("Character data not loaded. Please relog.", NamedTextColor.RED)
            )
            return
        }

        // Mutate and snapshot inside the same lock acquisition to avoid a race with the save loop.
        val snapshot =
            gamePlayer.withDocument {
                characters = characters - slot.toString()
                copy()
            }

        val saveResult = withContext(plugin.asyncDispatcher) { playerRepository.save(snapshot) }
        if (saveResult.isFailure) {
            logger.error(
                "FATAL: failed to save character deletion for player ${player.uniqueId} slot $slot",
                saveResult.exceptionOrNull()!!,
            )
            player.sendMessage(
                Component.text("Data save failed. Please reconnect.", NamedTextColor.RED)
            )
            return
        }

        val updatedTraits = userCharactersTraits - slot
        odalitaMenus.openMenu(characterSelectMenuFactory.create(updatedTraits), player)
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
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
