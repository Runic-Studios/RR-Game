package com.runicrealms.game.common.config.converter

import com.fasterxml.jackson.databind.util.StdConverter
import com.runicrealms.game.common.util.TextIcons
import com.runicrealms.game.common.util.colorFormat
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Jackson databind compatible converter for turning Strings into TextComponents.
 *
 * Supports both MiniMessage format (<gold>text</gold>) and legacy & colour codes (&6text). If the
 * string contains a '<' followed by a letter (indicating a MiniMessage tag), it is parsed with
 * MiniMessage. Otherwise, the legacy & serializer is used for backwards compatibility with older
 * config files that use & codes.
 */
class TextComponentConverter : StdConverter<String, TextComponent>() {

    companion object {
        private val MINI_MESSAGE = MiniMessage.miniMessage()
        private val MINI_MESSAGE_TAG_PATTERN = Regex("<[a-zA-Z/]")
    }

    override fun convert(value: String): TextComponent {
        val replaced = value.replace("<3", TextIcons.HEALTH_ICON)
        return if (MINI_MESSAGE_TAG_PATTERN.containsMatchIn(replaced)) {
            val parsed = MINI_MESSAGE.deserialize(replaced)
            // MiniMessage wraps its output in a TextComponent; cast is always safe here
            parsed as TextComponent
        } else {
            replaced.colorFormat()
        }
    }
}
