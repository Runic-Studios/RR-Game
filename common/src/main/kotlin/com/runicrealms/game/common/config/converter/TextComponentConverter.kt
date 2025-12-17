package com.runicrealms.game.common.config.converter

import com.fasterxml.jackson.databind.util.StdConverter
import com.runicrealms.game.common.util.TextIcons
import com.runicrealms.game.common.util.colorFormat
import net.kyori.adventure.text.TextComponent

/** Jackson databind compatible converter for turning Strings into TextComponents */
class TextComponentConverter : StdConverter<String, TextComponent>() {

    override fun convert(value: String): TextComponent {
        val replaced = value.replace("<3", TextIcons.HEALTH_ICON)
        return replaced.colorFormat()
    }
}
