package com.runicrealms.game.data.extension

import com.runicrealms.trove.generated.api.schema.v1.StatType
import net.kyori.adventure.text.format.NamedTextColor

data class StatTypeInfo(
    val name: String,
    val prefix: String,
    val color: NamedTextColor,
    val icon: String,
    val description: String
)

val statInfo = hashMapOf(
    StatType.DEXTERITY to StatTypeInfo("Dexterity", "DEX", NamedTextColor.YELLOW,
        "✦", "Gain spell haste, reducing your spell cooldowns!"),
    StatType.INTELLIGENCE to StatTypeInfo("Intelligence", "INT", NamedTextColor.DARK_AQUA,
        "ʔ", "Deal more magic damage and gain more mana regen!"),
    StatType.STRENGTH to StatTypeInfo("Strength", "STR", NamedTextColor.RED,
        "⚔", "Deal more physical damage!"),
    StatType.VITALITY to StatTypeInfo("Vitality", "VIT", NamedTextColor.WHITE,
        "■", "Gain damage reduction and health regen!"),
    StatType.WISDOM to StatTypeInfo("Wisdom", "WIS", NamedTextColor.GREEN,
        "✸", "Gain more spell healing, shielding, max mana and experience!")
)

fun StatType.getInfo(): StatTypeInfo {
    return statInfo[this] ?: throw IllegalArgumentException("Could not get info for stat $this")
}

