package com.runicrealms.game.items.config.perk

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.runicrealms.game.common.config.converter.TextComponentConverter
import java.util.Locale
import net.kyori.adventure.text.TextComponent

class GameItemPerkTemplate(
    identifier: String,
    @JsonDeserialize(contentConverter = TextComponentConverter::class)
    val name: TextComponent,
    @JsonProperty("max-stacks")
    val maxStacks: Int = 1,
    @JsonDeserialize(contentConverter = TextComponentConverter::class)
    val lore: List<TextComponent>? = null,
    val extraProperties: Map<String, Any> = mapOf(),
) {
    val identifier = identifier.lowercase(Locale.getDefault())
}