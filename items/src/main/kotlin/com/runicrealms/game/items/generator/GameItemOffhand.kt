package com.runicrealms.game.items.generator

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.extension.getInfo
import com.runicrealms.game.items.character.AddedStats
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.game.items.config.item.GameItemOffhandTemplate
import com.runicrealms.game.items.config.item.GameItemTemplate
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.perk.GameItemPerkHandlerRegistry
import com.runicrealms.game.items.util.ItemLoreBuilder
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import java.util.LinkedList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style

class GameItemOffhand
@AssistedInject
constructor(
    @Assisted inputData: ItemData,
    addedStatsFactory: AddedStats.Factory,
    templateRegistry: GameItemTemplateRegistry,
    private val perkTemplateRegistry: GameItemPerkTemplateRegistry,
    private val perkHandlerRegistry: GameItemPerkHandlerRegistry,
) :
    GameItem(inputData, templateRegistry.getItemTemplate(inputData.templateID)!!),
    AddedStatsHolder {

    val offhandTemplate = template as GameItemOffhandTemplate

    interface Factory {
        fun create(data: ItemData): GameItemOffhand
    }

    private var offhandData = data.offhand!!

    override val addedStats: AddedStats by lazy {
        // Store builder in case we need to update missing data (cross-check with template)
        val offhandDataBuilder by lazy { offhandData.toBuilder() }
        var modified = false

        // Correct and add stat bonuses
        val correctedStats = correctStatRolls(offhandData.statsList, offhandTemplate.stats)
        val correctedRolls = correctedStats.correctedRolls
        if (correctedStats.modified) {
            offhandDataBuilder.clearStats()
            offhandDataBuilder.addAllStats(correctedRolls)
            modified = true
        }

        // Correct and add item perks
        val correctedPerks = correctPerks(offhandData.perksList, offhandTemplate.defaultPerks)
        if (correctedPerks.modified) {
            offhandDataBuilder.clearPerks()
            offhandDataBuilder.addAllPerks(correctedPerks.correctedPerks)
            modified = true
        }

        if (modified) {
            // Update data
            offhandData = offhandDataBuilder.build()
            data = data.toBuilder().setOffhand(offhandData).build()
        }

        addedStatsFactory.create(correctedStats.calculatedRolls, offhandData.perksList, 0)
    }

    override fun generateLore(menuDisplay: Boolean): MutableList<TextComponent> {
        val statsData = offhandData.statsList

        val stats = mutableMapOf<StatType, Pair<ItemData.RolledStat, GameItemTemplate.StatRange>>()
        for ((statType, statRange) in offhandTemplate.stats) {
            stats[statType] = Pair(statsData.firstOrNull { it.type == statType }!!, statRange)
        }

        val statLore = LinkedList<TextComponent>()
        for (statType in StatType.entries) {
            val statRoll = stats[statType] ?: continue
            val statInfo = statType.getInfo()
            if (menuDisplay && statRoll.second.min != statRoll.second.max) {
                statLore.add(
                    Component.text(
                        "+" + statRoll.second.min + "-" + statRoll.second.max + statInfo.icon,
                        Style.style(statInfo.color),
                    )
                )
            } else {
                val value = statRoll.first.getRolledValue(statRoll.second)
                statLore.add(
                    Component.text(
                        ((if (value < 0) "-" else "+") + value + statInfo.icon),
                        Style.style(statInfo.color),
                    )
                )
            }
        }

        val perkLore = LinkedList<TextComponent>()
        var atLeastOnePerk = false
        for (perk in offhandData.perksList) {
            val perkTemplate = perkTemplateRegistry.getPerkTemplate(perk.perkID) ?: continue
            val handler = perkHandlerRegistry.getGameItemPerkHandler(perkTemplate) ?: continue
            val perkText =
                Component.text()
                    .append(
                        Component.text(
                            "<" + handler.getDynamicItemPerksStacksTextPlaceholder() + ">"
                        )
                    )
                    .append(
                        Component.text("+" + perk.stacks + " ", Style.style(NamedTextColor.WHITE))
                    )
                    .append(handler.getName())
                    .build()
            perkLore.add(perkText)
            val handlerLore = handler.getLoreSection()
            perkLore.addAll(handlerLore)
            perkLore.add(Component.text(""))
            atLeastOnePerk = true
        }
        if (atLeastOnePerk) perkLore.removeLast()

        return ItemLoreBuilder()
            .newLineIf(statLore.size > 0)
            .appendLinesIf(statLore.size > 0, statLore)
            .newLineIf(perkLore.size > 0)
            .appendLines(perkLore)
            .newLine()
            .appendLines(offhandTemplate.rarity.display)
            .appendLines(
                Component.text()
                    .append(Component.text("<level> "))
                    .append(Component.text("Lv. Min ", Style.style(NamedTextColor.GRAY)))
                    .append(
                        Component.text(
                            if (offhandTemplate.level > 0) offhandTemplate.level.toString()
                            else "None",
                            Style.style(NamedTextColor.WHITE),
                        )
                    )
                    .build()
            )
            .build()
    }
}
