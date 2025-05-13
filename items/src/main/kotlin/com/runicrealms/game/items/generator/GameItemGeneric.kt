package com.runicrealms.game.items.generator

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.items.config.template.GameItemGenericTemplate
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import net.kyori.adventure.text.TextComponent

class GameItemGeneric
@AssistedInject
constructor(@Assisted data: ItemData, templateRegistry: GameItemTemplateRegistry) :
    GameItem(data, templateRegistry.getTemplate(data.templateID)!!) {

    val genericTemplate = template as GameItemGenericTemplate

    interface Factory {
        fun create(data: ItemData): GameItemGeneric
    }

    override fun generateLore(menuDisplay: Boolean): MutableList<TextComponent> = mutableListOf()
}
