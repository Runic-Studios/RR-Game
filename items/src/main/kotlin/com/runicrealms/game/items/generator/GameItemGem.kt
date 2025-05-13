package com.runicrealms.game.items.generator

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.extension.getInfo
import com.runicrealms.game.items.config.template.GameItemGemTemplate
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.game.items.util.GemStatUtil
import com.runicrealms.game.items.util.ItemLoreBuilder
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

class GameItemGem @AssistedInject constructor(
    @Assisted inputData: ItemData,
    templateRegistry: GameItemTemplateRegistry,
) : GameItem(inputData, templateRegistry.getTemplate(inputData.templateID)!!) {

    val gemTemplate = template as GameItemGemTemplate

    interface Factory {
        fun create(data: ItemData): GameItemGem
    }

    override fun generateLore(menuDisplay: Boolean): MutableList<TextComponent> {
        val lore = mutableListOf<TextComponent>()

        for (stat in data.gem.bonus.statsList) {
            if (stat.amount == 0) continue
            val statInfo = stat.type.getInfo()
            lore.add(Component.text((if (stat.amount < 0) "-" else "+") + stat.amount + statInfo.icon, Style.style(statInfo.color)))
        }

        return ItemLoreBuilder()
            .appendLines(Component.text()
                .append(Component.text("Req Slots ", Style.style(NamedTextColor.GRAY)))
                .append(Component.text(GemStatUtil.getGemSlots(data.gem.bonus.tier)))
                .build())
            .newLine()
            .appendLines(lore)
            .newLine()
            .appendLines(
                Component.text("Drag and click on armor", Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC)),
                Component.text("to apply this gem!", Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC)))
            .build()
    }

}