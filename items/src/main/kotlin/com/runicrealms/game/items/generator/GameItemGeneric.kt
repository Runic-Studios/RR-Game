package com.runicrealms.game.items.generator

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.items.config.item.GameItemGenericTemplate
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import net.kyori.adventure.text.TextComponent

class GameItemGeneric
@AssistedInject
constructor(@Assisted data: ItemData, templateRegistry: GameItemTemplateRegistry) :
    GameItem(data, templateRegistry.getItemTemplate(data.templateID)!!) {

    val genericTemplate = template as GameItemGenericTemplate

    interface Factory {
        fun create(data: ItemData): GameItemGeneric
    }

    override fun generateLore(menuDisplay: Boolean): MutableList<TextComponent> = mutableListOf()
}
