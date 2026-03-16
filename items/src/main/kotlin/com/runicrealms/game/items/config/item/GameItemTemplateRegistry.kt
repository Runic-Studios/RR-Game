package com.runicrealms.game.items.config.item

import com.runicrealms.game.data.model.ItemData
import com.runicrealms.game.items.generator.GameItem

interface GameItemTemplateRegistry {

    fun getItemTemplate(identifier: String): GameItemTemplate?

    fun generateGameItem(template: GameItemTemplate) = generateGameItem(template.generateItemData())

    fun generateGameItem(itemData: ItemData): GameItem

    fun getItemTemplates(): Collection<GameItemTemplate>
}
