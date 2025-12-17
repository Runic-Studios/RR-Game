package com.runicrealms.game.data.menu

import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.menu.providers.MenuProviderLoader
import org.bukkit.entity.Player

class GamePlayerMenuProviderLoader
@Inject
constructor(private val userDataRegistry: UserDataRegistry) :
    MenuProviderLoader<GamePlayerMenuProvider> {
    override fun load(
        provider: GamePlayerMenuProvider,
        bukkitPlayer: Player,
        menuContents: MenuContents,
    ) {
        val character =
            userDataRegistry.getPlayer(bukkitPlayer.uniqueId)
                ?: throw IllegalStateException(
                    "Cannot open game player for player ${bukkitPlayer.uniqueId} when they don't have a player"
                )
        provider.load(character, menuContents)
    }
}
