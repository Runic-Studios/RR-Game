package com.runicrealms.game.gameplay.spell.skilltrees.util

import com.runicrealms.game.common.StatType
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell

/** Defines the perk lists for all three Cleric sub-classes. */
object ClericTreeUtil {

    val DEFAULT_CLERIC_SPELL_PERK = PerkSpell(perkId = 996, cost = 0, spellName = "Sacred Spring")

    fun bardPerkList(): List<Perk> =
        listOf(
            PerkSpell(36, 1, "Battlecry"),
            PerkBaseStat(37, 1, StatType.INTELLIGENCE, 5),
            PerkSpell(38, 1, "Accelerando"),
            PerkBaseStat(39, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(40, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(41, 1, "Powerslide"),
            PerkBaseStat(42, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(43, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(44, 1, StatType.DEXTERITY, 5),
            PerkSpell(45, 1, "Grand Symphony"),
            PerkBaseStat(46, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(47, 1, "Tempo"),
        )

    fun starweaverPerkList(): List<Perk> =
        listOf(
            PerkSpell(48, 1, "Starlight"),
            PerkBaseStat(49, 1, StatType.WISDOM, 5),
            PerkSpell(50, 1, "Astral Blessing"),
            PerkBaseStat(51, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(52, 1, StatType.WISDOM, 3),
            PerkSpell(53, 1, "Cosmic Prism"),
            PerkBaseStat(54, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(55, 1, StatType.WISDOM, 3),
            PerkBaseStat(56, 1, StatType.DEXTERITY, 5),
            PerkSpell(57, 1, "Nightfall"),
            PerkBaseStat(58, 1, StatType.WISDOM, 3),
            PerkSpell(59, 1, "Twilight Resurgence"),
        )

    fun lightbringerPerkList(): List<Perk> =
        listOf(
            PerkSpell(60, 1, "Sear"),
            PerkBaseStat(61, 1, StatType.WISDOM, 5),
            PerkSpell(62, 1, "Lightwell"),
            PerkBaseStat(63, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(64, 1, StatType.WISDOM, 3),
            PerkSpell(65, 1, "Radiant Nova"),
            PerkBaseStat(66, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(67, 1, StatType.WISDOM, 3),
            PerkBaseStat(68, 1, StatType.INTELLIGENCE, 5),
            PerkSpell(69, 1, "Ray Of Light"),
            PerkBaseStat(70, 1, StatType.WISDOM, 3),
            PerkSpell(71, 1, "Radiant Fire"),
        )
}
