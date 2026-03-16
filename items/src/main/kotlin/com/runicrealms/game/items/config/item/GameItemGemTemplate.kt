package com.runicrealms.game.items.config.item

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.runicrealms.game.common.StatType
import com.runicrealms.game.common.config.converter.TextComponentConverter
import com.runicrealms.game.data.model.GemBonus
import com.runicrealms.game.data.model.GemData
import com.runicrealms.game.data.model.ItemData
import com.runicrealms.game.items.config.jackson.converter.GameItemTagConverter
import com.runicrealms.game.items.config.jackson.converter.StatTypeConverter
import com.runicrealms.game.items.config.jackson.deserializer.GameItemClickTriggerTypeKeyDeserializer
import com.runicrealms.game.items.util.GemStatUtil
import net.kyori.adventure.text.TextComponent

class GameItemGemTemplate(
    @JsonProperty("id") id: String,
    @JsonProperty("display") display: DisplayableItem,
    @JsonProperty("tags")
    @JsonDeserialize(contentConverter = GameItemTagConverter::class)
    tags: List<GameItemTag> = listOf(),
    @JsonProperty("lore")
    @JsonDeserialize(contentConverter = TextComponentConverter::class)
    lore: List<TextComponent> = listOf(),
    @JsonProperty("triggers")
    @JsonDeserialize(
        `as` = LinkedHashMap::class,
        keyUsing = GameItemClickTriggerTypeKeyDeserializer::class,
    )
    triggers: LinkedHashMap<GameItemClickTrigger.Type, String> = LinkedHashMap(),
    @JsonProperty("extra") extraProperties: Map<String, Any> = mapOf(),
    @JsonProperty("tier") val tier: Int,
    @JsonProperty("main-stat")
    @JsonDeserialize(converter = StatTypeConverter::class)
    val mainStat: StatType,
) : GameItemTemplate(id, display, tags, lore, triggers, extraProperties) {

    override fun buildItemData(): ItemData {
        return ItemData(
            templateID = id,
            customData = emptyMap(),
            typeData =
                GemData(
                    bonus =
                        GemBonus(
                            stats = GemStatUtil.generateGemBonuses(tier, mainStat),
                            health = 0,
                            mainStat = mainStat,
                            tier = tier,
                        )
                ),
        )
    }
}
