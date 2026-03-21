package com.runicrealms.game.gameplay.spell.effect

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * Identifies each type of [SpellEffect]. Each entry carries a colour, display name, and an optional
 * symbol used in the [StackHologram].
 */
enum class SpellEffectType(val colour: TextColor, val displayName: String, val symbol: String) {
    ARCANUM(NamedTextColor.LIGHT_PURPLE, "Arcanum", ""),
    ARIA_OF_ARMOR(NamedTextColor.WHITE, "Aria of Armor", ""),
    BALLAD_OF_BINDING(NamedTextColor.YELLOW, "Ballad of Binding", ""),
    BETRAYED(NamedTextColor.RED, "Betrayed", ""),
    BLEED(NamedTextColor.DARK_RED, "Bleed", "☠"),
    BLESSED_BLADE(NamedTextColor.GREEN, "Blessed Blade", "⚔"),
    CHARGED(NamedTextColor.BLUE, "Charged", "➹"),
    CHILLED(NamedTextColor.AQUA, "Chilled", "❈"),
    HOLY_FERVOR(NamedTextColor.GOLD, "Holy Fervor", ""),
    ICE_BARRIER(NamedTextColor.WHITE, "Ice Barrier", "✲"),
    IGNITED(NamedTextColor.DARK_RED, "Ignited", ""),
    INCENDIARY(NamedTextColor.DARK_RED, "Incendiary", ""),
    RADIANT_FIRE(NamedTextColor.YELLOW, "Radiant Fire", "☀"),
    SHROUDED(NamedTextColor.DARK_GRAY, "Shrouded", ""),
    SONG_OF_WAR(NamedTextColor.RED, "Song of War", ""),
    STATIC(NamedTextColor.GRAY, "Static", ""),
    SUNDERED(NamedTextColor.BLUE, "Sundered", "■"),
}
