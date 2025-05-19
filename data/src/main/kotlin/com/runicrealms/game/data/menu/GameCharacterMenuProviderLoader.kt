package com.runicrealms.game.data.menu

import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.menu.providers.MenuProviderLoader
import org.bukkit.entity.Player

class GameCharacterMenuProviderLoader
@Inject
constructor(private val userDataRegistry: UserDataRegistry) :
    MenuProviderLoader<GameCharacterMenuProvider> {
    override fun load(
        provider: GameCharacterMenuProvider,
        bukkitPlayer: Player,
        menuContents: MenuContents,
    ) {
        val character =
            userDataRegistry.getCharacter(bukkitPlayer.uniqueId)
                ?: throw IllegalStateException(
                    "Cannot open game character for player ${bukkitPlayer.uniqueId} when they don't have a character"
                )
        provider.load(character, menuContents)
    }
}
