package com.runicrealms.game.items.config.template

import com.runicrealms.game.items.generator.GameItem

interface GameItemTemplateRegistry {

    fun getTemplate(identifier: String): GameItemTemplate?

    fun GameItemTemplate.generateGameItem(count: Int): GameItem
}
