package com.runicrealms.game.items.config.jackson.converter

import com.fasterxml.jackson.databind.util.StdConverter
import com.runicrealms.game.items.config.template.GameItemRarityType

/** Jackson databind compatible converter for turning Strings into ItemRarityTypes */
class GameItemRarityTypeConverter : StdConverter<String, GameItemRarityType?>() {

    override fun convert(value: String): GameItemRarityType {
        for (type in GameItemRarityType.entries) {
            if (type.identifier.equals(value, ignoreCase = true)) {
                return type
            }
        }
        throw IllegalArgumentException("Unknown rarity type: $value")
    }
}
