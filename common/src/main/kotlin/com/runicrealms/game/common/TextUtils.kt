package com.runicrealms.game.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.util.ChatPaginator

const val LINE_LENGTH = 28

fun Player.sendError(message: String) {
    sendMessage(Component.text(message).color(TextColor.color(0xbf0202)))
}

fun String.colorFormat(altChar: Char = '&'): TextComponent =
    LegacyComponentSerializer.legacy(altChar).deserialize(this)

fun TextComponent.toLegacy(altChar: Char = '&'): String =
    LegacyComponentSerializer.legacy(altChar).serialize(this)

object TextIcons {
    const val HEALTH_ICON: String = "❤"
    const val EMPTY_GEM_ICON: String = "◇"
}

fun String.breakLines(lineLength: Int = LINE_LENGTH): List<String> {
    return listOf(*ChatPaginator.wordWrap(this, lineLength))
}
