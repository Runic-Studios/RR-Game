package com.runicrealms.game.items.config.item

import com.runicrealms.game.items.generator.GameItem
import com.runicrealms.trove.generated.api.schema.v1.ItemData

interface GameItemTemplateRegistry {

    fun getItemTemplate(identifier: String): GameItemTemplate?

    fun generateGameItem(template: GameItemTemplate) = generateGameItem(template.generateItemData())

    fun generateGameItem(itemData: ItemData): GameItem

    fun getItemTemplates(): Collection<GameItemTemplate>
}
