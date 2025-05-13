package com.runicrealms.game.items.config.jackson.converter

import com.fasterxml.jackson.databind.util.StdConverter
import com.runicrealms.game.items.config.template.GameItemTag

/**
 * Jackson databind compatible converter for turning Strings into GameItemTags
 */
class GameItemTagConverter : StdConverter<String, GameItemTag?>() {

    override fun convert(value: String): GameItemTag? =
        GameItemTag.getFromIdentifier(value)

}