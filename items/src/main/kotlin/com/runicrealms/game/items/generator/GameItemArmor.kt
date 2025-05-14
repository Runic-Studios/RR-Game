package com.runicrealms.game.items.generator

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.TextIcons
import com.runicrealms.game.data.extension.getInfo
import com.runicrealms.game.items.character.AddedStats
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.game.items.config.template.GameItemArmorTemplate
import com.runicrealms.game.items.config.template.GameItemTemplate
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.game.items.perk.GameItemPerkHandlerRegistry
import com.runicrealms.game.items.util.GemStatUtil
import com.runicrealms.game.items.util.ItemLoreBuilder
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import java.util.LinkedList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

class GameItemArmor
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

    val armorTemplate = template as GameItemArmorTemplate

    interface Factory {
        fun create(data: ItemData): GameItemArmor
    }

    private var armorData = data.armor!!

    override val addedStats: AddedStats by lazy {
        // Store builder in case we need to update missing data (cross-check with template)
        val armorDataBuilder by lazy { armorData.toBuilder() }
        var modified = false

        // Correct and add stat bonuses
        val correctedStats = correctStatRolls(armorData.statsList, armorTemplate.stats)
        val correctedRolls = correctedStats.correctedRolls
        val calculatedStats = correctedStats.calculatedRolls
        if (correctedStats.modified) {
            armorDataBuilder.clearStats()
            armorDataBuilder.addAllStats(correctedRolls)
            modified = true
        }

        // Add gem bonuses
        var bonusHealth = armorTemplate.health
        for (gemBonus in armorData.gemBonusesList) {
            for (stat in gemBonus.statsList) {
                calculatedStats[stat.type] =
                    calculatedStats.getOrDefault(stat.type, 0) + stat.amount
            }
            bonusHealth += gemBonus.health
        }

        // Correct and add item perks
        val correctedPerks = correctPerks(armorData.perksList, armorTemplate.defaultPerks)
        if (correctedPerks.modified) {
            armorDataBuilder.clearPerks()
            armorDataBuilder.addAllPerks(correctedPerks.correctedPerks)
            modified = true
        }

        if (modified) {
            // Update data
            armorData = armorDataBuilder.build()
            data = data.toBuilder().setArmor(armorData).build()
        }

        addedStatsFactory.create(calculatedStats, armorData.perksList, bonusHealth)
    }

    override fun generateLore(menuDisplay: Boolean): MutableList<TextComponent> {
        val builder = ItemLoreBuilder()

        val statsData = armorData.statsList

        val stats = mutableMapOf<StatType, Pair<ItemData.RolledStat, GameItemTemplate.StatRange>>()
        for ((statType, statRange) in armorTemplate.stats) {
            stats[statType] = Pair(statsData.firstOrNull { it.type == statType }!!, statRange)
        }

        val gemOnlyStats = HashMap<StatType, Int>()
        for (gemBonus in armorData.gemBonusesList) {
            for (gemStat in gemBonus.statsList) {
                if (stats.containsKey(gemStat.type)) continue // This is added later
                gemOnlyStats[gemStat.type] =
                    gemOnlyStats.getOrDefault(gemStat.type, 0) + gemStat.amount
            }
        }

        val statLore = LinkedList<TextComponent>()
        for (statType in StatType.entries) {
            val statRoll = stats[statType]
            val statInfo = statType.getInfo()
            if (menuDisplay && statRoll != null && statRoll.second.min != statRoll.second.max) {
                statLore.add(
                    Component.text(
                        "+" + statRoll.second.min + "-" + statRoll.second.max + statInfo.icon,
                        Style.style(statInfo.color),
                    )
                )
            } else if (statRoll != null) {
                val value = statRoll.first.getRolledValue(statRoll.second)
                var finalValue = value
                for (gemBonus in armorData.gemBonusesList) {
                    val gemMatch =
                        gemBonus.statsList.firstOrNull { it.type == statType } ?: continue
                    finalValue += gemMatch.amount
                }
                if (finalValue == value) {
                    statLore.add(
                        Component.text(
                            ((if (value < 0) "-" else "+") + value + statInfo.icon),
                            Style.style(statInfo.color),
                        )
                    )
                } else {
                    val originalText =
                        Component.text(
                            (if (value < 0) "-" else "+") + value + statInfo.icon,
                            Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH),
                        )
                    val newText =
                        Component.text(
                            (if (finalValue < 0) "-" else "+") + finalValue + statInfo.icon,
                            Style.style(statInfo.color),
                        )
                    statLore.add(
                        Component.text()
                            .append(originalText)
                            .append(Component.text(" "))
                            .append(newText)
                            .build()
                    )
                }
            } else if (gemOnlyStats.containsKey(statType)) {
                val value = gemOnlyStats[statType]!!
                val originalText =
                    Component.text(
                        "+0" + statInfo.icon,
                        Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH),
                    )
                val newText =
                    Component.text(
                        (if (value < 0) "-" else "+") + value + statInfo.icon,
                        Style.style(statInfo.color),
                    )
                statLore.add(
                    Component.text()
                        .append(originalText)
                        .append(Component.text(" "))
                        .append(newText)
                        .build()
                )
            }
        }

        val health = armorTemplate.health
        var finalHealth = health
        for (gemBonus in armorData.gemBonusesList) {
            finalHealth += gemBonus.health
        }
        val healthText =
            if (finalHealth == health) {
                Component.text(
                    health.toString() + TextIcons.HEALTH_ICON,
                    Style.style(NamedTextColor.RED),
                )
            } else {
                val originalText =
                    Component.text(
                        health.toString() + TextIcons.HEALTH_ICON,
                        Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH),
                    )
                val newText =
                    Component.text(
                        originalText.toString() + TextIcons.HEALTH_ICON,
                        Style.style(NamedTextColor.RED),
                    )
                Component.text()
                    .append(originalText)
                    .append(Component.text(" "))
                    .append(newText)
                    .build()
            }

        val gemTextBuilder =
            Component.text()
                .append(Component.text("Gem Slots: ", Style.style(NamedTextColor.GRAY)))
                .append(Component.text("[ ", Style.style(NamedTextColor.WHITE)))
        var counter = 0
        for (gemBonus in armorData.gemBonusesList) {
            for (i in 0..<GemStatUtil.getGemSlots(gemBonus.tier)) {
                val gemInfo = gemBonus.mainStat.getInfo()
                gemTextBuilder.append(
                    Component.text(gemInfo.icon + " ", Style.style(gemInfo.color))
                )
                counter++
            }
        }
        for (i in counter..<armorTemplate.maxGemSlots) {
            gemTextBuilder.append(
                Component.text(TextIcons.EMPTY_GEM_ICON + " ", Style.style(NamedTextColor.GRAY))
            )
        }
        gemTextBuilder.append(Component.text("]", Style.style(NamedTextColor.WHITE)))

        builder
            .appendLinesIf(armorTemplate.maxGemSlots > 0, gemTextBuilder.build())
            .newLine()
            .appendLines(healthText)
            .newLine()
            .appendLinesIf(statLore.size > 0, statLore)
            .newLineIf(statLore.size > 0)

        val perkLore = LinkedList<TextComponent>()
        var atLeastOnePerk = false
        for (perk in armorData.perksList) {
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
            if (handlerLore != null) perkLore.addAll(handlerLore)
            perkLore.add(Component.text(""))
            atLeastOnePerk = true
        }
        if (atLeastOnePerk) perkLore.removeLast()

        builder
            .appendLinesIf(atLeastOnePerk, perkLore)
            .newLineIf(atLeastOnePerk)
            .appendLines(
                Component.text()
                    .append(armorTemplate.rarity.display)
                    .append(
                        Component.text(
                            " " + getArmorName(),
                            Style.style(armorTemplate.rarity.color),
                        )
                    )
                    .build()
            )
            .appendLines(
                Component.text()
                    .append(Component.text("<class> "))
                    .append(
                        Component.text(
                            armorTemplate.classType.getInfo().name,
                            Style.style(NamedTextColor.GRAY),
                        )
                    )
                    .build()
            )
            .appendLines(
                Component.text()
                    .append(Component.text("<level> "))
                    .append(Component.text("Lv. Min ", Style.style(NamedTextColor.GRAY)))
                    .append(
                        Component.text(
                            if (armorTemplate.level > 0) armorTemplate.level.toString() else "None",
                            Style.style(NamedTextColor.WHITE),
                        )
                    )
                    .build()
            )

        return builder.build()
    }

    private fun getArmorName(): String {
        val materialName = armorTemplate.display.material.name
        if (materialName.contains("HELMET")) return "Helmet"
        if (materialName.contains("CHESTPLATE")) return "Chestplate"
        if (materialName.contains("LEGGINGS")) return "Leggings"
        if (materialName.contains("BOOTS")) return "Boots"
        return ""
    }
}
