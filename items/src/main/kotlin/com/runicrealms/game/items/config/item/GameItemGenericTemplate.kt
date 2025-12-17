package com.runicrealms.game.items.config.item

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.runicrealms.game.common.config.converter.TextComponentConverter
import com.runicrealms.game.items.config.jackson.converter.GameItemTagConverter
import com.runicrealms.game.items.config.jackson.deserializer.GameItemClickTriggerTypeKeyDeserializer
import net.kyori.adventure.text.TextComponent

class GameItemGenericTemplate(
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
) : GameItemTemplate(id, display, tags, lore, triggers, extraProperties)
