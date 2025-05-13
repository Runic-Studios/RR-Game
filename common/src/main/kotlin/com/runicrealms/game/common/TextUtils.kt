package com.runicrealms.game.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

fun Player.sendError(message: String) {
    sendMessage(Component.text(message).color(TextColor.color(0xbf0202)))
}

fun String.colorFormat(altChar: Char = '&'): TextComponent = LegacyComponentSerializer.legacy(altChar).deserialize(this)

object TextIcons {
    const val HEALTH_ICON: String = "❤"
    const val EMPTY_GEM_ICON: String = "◇"
}