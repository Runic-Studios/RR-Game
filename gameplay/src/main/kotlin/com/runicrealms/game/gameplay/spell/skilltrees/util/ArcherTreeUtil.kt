package com.runicrealms.game.gameplay.spell.skilltrees.util

import com.runicrealms.game.common.StatType
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell

/** Defines the perk lists for all three Archer sub-classes. */
object ArcherTreeUtil {

    val DEFAULT_ARCHER_SPELL_PERK = PerkSpell(perkId = 995, cost = 0, spellName = "Rapid Fire")

    fun marksmanPerkList(): List<Perk> =
        listOf(
            PerkSpell(0, 1, "Piercing Arrow"),
            PerkBaseStat(1, 1, StatType.STRENGTH, 5),
            PerkSpell(2, 1, "Ambush"),
            PerkBaseStat(3, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(4, 1, StatType.STRENGTH, 3),
            PerkSpell(5, 1, "Leaping Shot"),
            PerkBaseStat(6, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(7, 1, StatType.STRENGTH, 3),
            PerkBaseStat(9, 1, StatType.DEXTERITY, 5),
            PerkSpell(8, 1, "Rain of Arrows"),
            PerkBaseStat(10, 1, StatType.STRENGTH, 3),
            PerkSpell(11, 1, "Steady Aim"),
        )

    fun wardenPerkList(): List<Perk> =
        listOf(
            PerkSpell(12, 1, "Snare Trap"),
            PerkBaseStat(13, 1, StatType.WISDOM, 5),
            PerkSpell(14, 1, "Refreshing Volley"),
            PerkBaseStat(15, 1, StatType.STRENGTH, 3),
            PerkBaseStat(16, 1, StatType.WISDOM, 3),
            PerkSpell(17, 1, "Remedy"),
            PerkBaseStat(18, 1, StatType.STRENGTH, 3),
            PerkBaseStat(19, 1, StatType.WISDOM, 3),
            PerkBaseStat(21, 1, StatType.STRENGTH, 5),
            PerkSpell(20, 1, "Sacred Grove"),
            PerkBaseStat(22, 1, StatType.WISDOM, 3),
            PerkSpell(23, 1, "Gifts Of The Grove"),
        )

    fun stormshotPerkList(): List<Perk> =
        listOf(
            PerkSpell(24, 1, "Thunder Arrow"),
            PerkBaseStat(25, 1, StatType.INTELLIGENCE, 5),
            PerkSpell(26, 1, "Stormborn"),
            PerkBaseStat(27, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(28, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(29, 1, "Surge"),
            PerkBaseStat(30, 1, StatType.DEXTERITY, 3),
            PerkBaseStat(31, 1, StatType.INTELLIGENCE, 3),
            PerkBaseStat(33, 1, StatType.DEXTERITY, 5),
            PerkSpell(32, 1, "Jolt"),
            PerkBaseStat(34, 1, StatType.INTELLIGENCE, 3),
            PerkSpell(35, 1, "Overcharge"),
        )
}
