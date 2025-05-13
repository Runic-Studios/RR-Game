package com.runicrealms.game.items.config.template

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.runicrealms.game.common.config.converter.TextComponentConverter
import com.runicrealms.game.items.config.jackson.converter.GameItemTagConverter
import com.runicrealms.game.items.config.jackson.converter.StatTypeConverter
import com.runicrealms.game.items.config.jackson.deserializer.GameItemClickTriggerTypeKeyDeserializer
import com.runicrealms.game.items.util.GemStatUtil
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import net.kyori.adventure.text.TextComponent

class GameItemGemTemplate(
    @JsonProperty("id")
    id: String,

    @JsonProperty("display")
    display: DisplayableItem,

    @JsonProperty("tags")
    @JsonDeserialize(contentConverter = GameItemTagConverter::class)
    tags: List<GameItemTag> = listOf(),

    @JsonProperty("lore")
    @JsonDeserialize(contentConverter = TextComponentConverter::class)
    lore: List<TextComponent> = listOf(),

    @JsonProperty("triggers")
    @JsonDeserialize(
        `as` = LinkedHashMap::class,
        keyUsing = GameItemClickTriggerTypeKeyDeserializer::class
    )
    triggers: LinkedHashMap<GameItemClickTrigger.Type, String> = LinkedHashMap(),

    @JsonProperty("extra")
    extraProperties: Map<String, Any> = mapOf(),

    @JsonProperty("tier")
    val tier: Int,

    @JsonProperty("main-stat")
    @JsonDeserialize(contentConverter = StatTypeConverter::class)
    val mainStat: StatType
) : GameItemTemplate(id, display, tags, lore, triggers, extraProperties) {

    override fun buildItemData(count: Int): ItemData.Builder {
        val builder = super.buildItemData(count)
        val gemBuilder = builder.gemBuilder

        val bonus = ItemData.GemBonus.newBuilder()
            .setHealth(0)
            .setTier(tier)
            .setMainStat(mainStat)
            .addAllStats(GemStatUtil.generateGemBonuses(tier, mainStat))
            .build()
        gemBuilder.setBonus(bonus)
        builder.setGem(gemBuilder.build())
        return builder
    }

}