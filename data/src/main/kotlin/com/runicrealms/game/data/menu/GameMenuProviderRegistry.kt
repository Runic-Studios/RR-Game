package com.runicrealms.game.data.menu

import com.google.inject.Inject
import nl.odalitadevelopments.menus.OdalitaMenus

class GameMenuProviderRegistry
@Inject
constructor(
    odalitaMenus: OdalitaMenus,
    gamePlayerMenuProviderLoader: GamePlayerMenuProviderLoader,
    gameCharacterMenuProviderLoader: GameCharacterMenuProviderLoader,
) {

    init {
        odalitaMenus.registerProviderLoader(
            GamePlayerMenuProvider::class.java,
            gamePlayerMenuProviderLoader,
        )
        odalitaMenus.registerProviderLoader(
            GameCharacterMenuProvider::class.java,
            gameCharacterMenuProviderLoader,
        )
    }
}
