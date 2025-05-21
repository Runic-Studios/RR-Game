package com.runicrealms.game.items.config.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style

enum class GameItemRarityType(val identifier: String, val color: NamedTextColor, val text: String) {
    COMMON("common", NamedTextColor.GRAY, "Common"),
    UNCOMMON("uncommon", NamedTextColor.GREEN, "Uncommmon"),
    RARE("rare", NamedTextColor.AQUA, "Rare"),
    EPIC("epic", NamedTextColor.LIGHT_PURPLE, "Epic"),
    LEGENDARY("legendary", NamedTextColor.GOLD, "Legendary"),
    UNIQUE("unique", NamedTextColor.YELLOW, "Unique"),
    CRAFTED("crafted", NamedTextColor.WHITE, "Crafted");

    val display = Component.text(text, Style.style(color))

    companion object {
        fun getFromIdentifier(identifier: String): GameItemRarityType? {
            for (rarity in entries) {
                if (rarity.identifier.equals(identifier, ignoreCase = true)) {
                    return rarity
                }
            }
            return null
        }
    }
}
