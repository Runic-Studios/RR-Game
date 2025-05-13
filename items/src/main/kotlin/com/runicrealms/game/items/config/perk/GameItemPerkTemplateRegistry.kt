package com.runicrealms.game.items.config.perk

interface GameItemPerkTemplateRegistry {

    fun getGameItemPerkTemplate(identifier: String): GameItemPerkTemplate?
}
