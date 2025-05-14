package com.runicrealms.game.items.config.template

import com.runicrealms.game.items.generator.GameItem
import com.runicrealms.trove.generated.api.schema.v1.ItemData

interface GameItemTemplateRegistry {

    fun getTemplate(identifier: String): GameItemTemplate?

    fun GameItemTemplate.generateGameItem(count: Int): GameItem = generateGameItem(generateItemData(count))

    fun generateGameItem(template: GameItemTemplate, count: Int) = template.generateGameItem(count)

    fun generateGameItem(itemData: ItemData): GameItem
}
