package com.runicrealms.game.items.generator

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.StatType
import com.runicrealms.game.data.extension.getInfo
import com.runicrealms.game.data.model.ItemData
import com.runicrealms.game.data.model.OffhandData
import com.runicrealms.game.items.character.AddedStats
import com.runicrealms.game.items.config.item.GameItemOffhandTemplate
import com.runicrealms.game.items.config.item.GameItemTemplate
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.game.items.perk.GameItemPerkHandlerRegistry
import com.runicrealms.game.items.util.ItemLoreBuilder
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

    private var offhandData = data.typeData as OffhandData

    override val addedStats: AddedStats by lazy {
        var modified = false

        val correctedStats = correctStatRolls(offhandData.stats, offhandTemplate.stats)
        if (correctedStats.modified) {
            offhandData = offhandData.copy(stats = correctedStats.correctedRolls)
            modified = true
        }

        val correctedPerks = correctPerks(offhandData.perks, offhandTemplate.defaultPerks)
        if (correctedPerks.modified) {
            offhandData = offhandData.copy(perks = correctedPerks.correctedPerks)
            modified = true
        }

        if (modified) {
            data = data.copy(typeData = offhandData)
        }

        addedStatsFactory.create(
            correctedStats.calculatedRolls,
            offhandData.perks.toMutableList(),
            0,
        )
    }

    override fun generateLore(menuDisplay: Boolean): MutableList<TextComponent> {
        val statsData = offhandData.stats

        val stats =
            mutableMapOf<
                StatType,
                Pair<com.runicrealms.game.data.model.RolledStat, GameItemTemplate.StatRange>,
            >()
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
        for (perk in offhandData.perks) {
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
