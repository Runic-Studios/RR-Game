package com.runicrealms.game.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

fun Player.sendError(message: String) {
    sendMessage(Component.text(message).color(TextColor.color(0xbf0202)))
}
