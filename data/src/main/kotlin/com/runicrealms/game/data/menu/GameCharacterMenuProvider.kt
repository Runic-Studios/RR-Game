package com.runicrealms.game.data.menu

import com.runicrealms.game.data.game.GameCharacter
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.menu.providers.MenuProvider

interface GameCharacterMenuProvider : MenuProvider {

    fun load(character: GameCharacter, menuContents: MenuContents)
}
