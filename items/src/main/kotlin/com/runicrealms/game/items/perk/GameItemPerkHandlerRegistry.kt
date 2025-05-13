package com.runicrealms.game.items.perk

import com.runicrealms.game.items.config.perk.GameItemPerkTemplate

interface GameItemPerkHandlerRegistry {

    fun getGameItemPerkHandler(template: GameItemPerkTemplate): GameItemPerkHandler?

    fun registerItemPerk(handler: GameItemPerkHandler)
}
