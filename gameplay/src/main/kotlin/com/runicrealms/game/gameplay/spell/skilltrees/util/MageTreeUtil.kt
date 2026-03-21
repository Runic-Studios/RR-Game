package com.runicrealms.game.gameplay.spell.skilltrees.util

import com.runicrealms.game.common.StatType
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell

/** Defines the perk lists for all three Mage sub-classes. */
object MageTreeUtil {

    val DEFAULT_MAGE_SPELL_PERK = PerkSpell(perkId = 997, cost = 0, spellName = "Fireball")

    fun cryomancerPerkList(): List<Perk> =
        listOf(
            PerkSpell(72, 1, "Frostbite"),
            PerkBaseStat(73, 1, StatType.INTELLIGENCE, 5),
            PerkSpell(74, 1, "Shatter"),
            PerkBaseStat(75, 1, StatType.VITALITY, 3),
            PerkBaseStat(76, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(77, 1, "Snap Freeze"),
            PerkBaseStat(78, 1, StatType.VITALITY, 3),
            PerkBaseStat(79, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(80, 1, StatType.VITALITY, 5),
            PerkSpell(81, 1, "Blizzard"),
            PerkBaseStat(82, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(83, 1, "Glacier"),
        )

    fun pyromancerPerkList(): List<Perk> =
        listOf(
            PerkSpell(84, 1, "Dragon's Breath"),
            PerkBaseStat(85, 1, StatType.INTELLIGENCE, 5),
            PerkSpell(86, 1, "Incendiary"),
            PerkBaseStat(87, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(88, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(89, 1, "Erupt"),
            PerkBaseStat(90, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(91, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(92, 1, StatType.DEXTERITY, 5),
            PerkSpell(93, 1, "Meteor"),
            PerkBaseStat(94, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(95, 1, "Wildfire"),
        )

    fun spellswordPerkList(): List<Perk> =
        listOf(
            PerkSpell(96, 1, "Arcane Slash"),
            PerkBaseStat(97, 1, StatType.WISDOM, 5),
            PerkSpell(98, 1, "Spectral Blade"),
            PerkBaseStat(99, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(100, 1, StatType.WISDOM, 3),
            PerkSpell(101, 1, "Blink"),
            PerkBaseStat(102, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(103, 1, StatType.WISDOM, 3),
            PerkBaseStat(104, 1, StatType.INTELLIGENCE, 5),
            PerkSpell(105, 1, "Primal Arcanum"),
            PerkBaseStat(106, 1, StatType.WISDOM, 3),
            PerkSpell(107, 1, "Riftwalk"),
        )
}
