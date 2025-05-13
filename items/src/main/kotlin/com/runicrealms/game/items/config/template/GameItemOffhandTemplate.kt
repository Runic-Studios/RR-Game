package com.runicrealms.game.items.config.template

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.runicrealms.game.common.config.converter.TextComponentConverter
import com.runicrealms.game.items.config.jackson.converter.GameItemRarityTypeConverter
import com.runicrealms.game.items.config.jackson.converter.GameItemTagConverter
import com.runicrealms.game.items.config.jackson.deserializer.GameItemClickTriggerTypeKeyDeserializer
import com.runicrealms.game.items.config.jackson.deserializer.StatTypeKeyDeserializer
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import net.kyori.adventure.text.TextComponent

class GameItemOffhandTemplate(
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
    @JsonProperty("stats")
    @JsonDeserialize(keyUsing = StatTypeKeyDeserializer::class, `as` = LinkedHashMap::class)
    val stats: LinkedHashMap<StatType, StatRange> = LinkedHashMap(),
    @JsonProperty("item-perks")
    @JsonDeserialize(`as` = LinkedHashMap::class)
    val defaultPerks: LinkedHashMap<String, Int> = LinkedHashMap(),
    @JsonProperty("level") val level: Int,
    @JsonProperty("rarity")
    @JsonDeserialize(contentConverter = GameItemRarityTypeConverter::class)
    val rarity: GameItemRarityType,
) : GameItemTemplate(id, display, tags, lore, triggers, extraProperties) {

    override fun buildItemData(count: Int): ItemData.Builder {
        val builder = super.buildItemData(count)
        val offhandBuilder =
            builder.offhandBuilder
                .addAllStats(stats.toRolledStats())
                .addAllPerks(defaultPerks.toPerks())
        builder.setOffhand(offhandBuilder.build())
        return builder
    }
}
