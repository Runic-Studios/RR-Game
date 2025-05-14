package com.runicrealms.game.items.config.perk

interface GameItemPerkTemplateRegistry {

    fun getPerkTemplate(identifier: String): GameItemPerkTemplate?

    fun getPerkTemplates(): Collection<GameItemPerkTemplate>
}
