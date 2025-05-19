package com.runicrealms.game.data.menu

import com.runicrealms.game.data.game.GamePlayer
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.menu.providers.MenuProvider

interface GamePlayerMenuProvider : MenuProvider {

    fun load(player: GamePlayer, menuContents: MenuContents)
}
