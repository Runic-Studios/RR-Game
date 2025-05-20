package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.trove.client.user.UserCharactersTraits
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.util.function.Supplier
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.contents.action.MenuCloseResult
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.items.buttons.OpenMenuItem
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@Menu(title = "Choose Your Class", type = MenuType.CHEST_1_ROW)
class CharacterAddMenu
@AssistedInject
constructor(
    private val characterSelectHelper: CharacterSelectHelper,
    private val characterSelectManager: CharacterSelectManager,
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    @Assisted private val slot: Int,
    @Assisted private val userCharactersTraits: UserCharactersTraits,
) : PlayerMenuProvider {

    interface Factory {
        fun create(slot: Int, userCharactersTraits: UserCharactersTraits): CharacterAddMenu
    }

    @Inject private lateinit var characterSelectMenuFactory: CharacterSelectMenu.Factory

    @Volatile private var hasSelected = false

    private fun createChooseClassIcon(classType: ClassType): ClickableItem {
        return ClickableItem.of(characterSelectHelper.classIcons[classType]!!) { event ->
            if (hasSelected) return@of
            hasSelected = true
            event.whoClicked.closeInventory()
            characterSelectManager.creationCharacterTypes[event.whoClicked.uniqueId] = classType
            event.whoClicked.sendMessage("&aCreating your character...")
            plugin.launch { userDataRegistry.setCharacter(event.whoClicked.uniqueId, slot) }
        }
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        menuContents.set(
            0,
            OpenMenuItem.of(
                characterSelectHelper.goBackItem,
                characterSelectMenuFactory.create(userCharactersTraits),
            ),
        )
        menuContents.set(2, createChooseClassIcon(ClassType.ARCHER))
        menuContents.set(3, createChooseClassIcon(ClassType.CLERIC))
        menuContents.set(4, createChooseClassIcon(ClassType.MAGE))
        menuContents.set(5, createChooseClassIcon(ClassType.ROGUE))
        menuContents.set(6, createChooseClassIcon(ClassType.WARRIOR))
        menuContents
            .events()
            .onClose(
                Supplier<MenuCloseResult> {
                    if (hasSelected) MenuCloseResult.CLOSE else MenuCloseResult.KEEP_OPEN
                }
            )
    }
}
