package com.runicrealms.game.items.config.template

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style

enum class GameItemTag(val identifier: String, val display: TextComponent) {
    CONSUMABLE("consumable", Component.text("Consumable", Style.style(NamedTextColor.GRAY))),
    FOOD("food", Component.text("Food", Style.style(NamedTextColor.YELLOW))),
    POTION("potion", Component.text("Potion", Style.style(NamedTextColor.BLUE))),
    SOULBOUND("soulbound", Component.text("Soulbound", Style.style(NamedTextColor.DARK_GRAY))),
    UNTRADEABLE("untradeable", Component.text("Untradeable", Style.style(NamedTextColor.DARK_RED))),
    QUEST_ITEM("quest-item", Component.text("Quest Item", Style.style(NamedTextColor.GOLD))),
    DUNGEON_ITEM("dungeon-item", Component.text("Dungeon Item", Style.style(NamedTextColor.RED)));

    companion object {
        fun getFromIdentifier(identifier: String): GameItemTag? {
            for (tag in entries) {
                if (tag.identifier.equals(identifier, ignoreCase = true)) {
                    return tag
                }
            }
            return null
        }
    }
}
