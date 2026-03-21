package com.runicrealms.game.gameplay.spell.skilltrees.util

import com.runicrealms.game.common.StatType
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell

/** Defines the perk lists for all three Warrior sub-classes. */
object WarriorTreeUtil {

    /**
     * TODO: perkId 997 is also used by MageTreeUtil.DEFAULT_MAGE_SPELL_PERK — known conflict from
     *   original codebase.
     */
    val DEFAULT_WARRIOR_SPELL_PERK = PerkSpell(perkId = 999, cost = 0, spellName = "Slam")

    fun berserkerPerkList(): List<Perk> =
        listOf(
            PerkSpell(144, 1, "Cleave"),
            PerkBaseStat(145, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkSpell(146, 1, "Rupture"),
            PerkBaseStat(147, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkBaseStat(148, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkSpell(149, 1, "Axe Toss"),
            PerkBaseStat(150, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkBaseStat(151, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkBaseStat(152, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkSpell(153, 1, "Adrenaline"),
            PerkBaseStat(154, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkSpell(155, 1, "Bloodbath"),
        )

    fun dreadlordPerkList(): List<Perk> =
        listOf(
            PerkSpell(156, 1, "Devour"),
            PerkBaseStat(157, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkSpell(158, 1, "Soul Reaper"),
            PerkBaseStat(159, 1, StatType.INTELLIGENCE, maxAllocatedPoints = 3),
            PerkBaseStat(160, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkSpell(161, 1, "Umbral Grasp"),
            PerkBaseStat(162, 1, StatType.INTELLIGENCE, maxAllocatedPoints = 3),
            PerkBaseStat(163, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkBaseStat(164, 1, StatType.INTELLIGENCE, maxAllocatedPoints = 3),
            PerkSpell(165, 1, "Damnation"),
            PerkBaseStat(166, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkSpell(167, 1, "Ruination"),
        )

    fun paladinPerkList(): List<Perk> =
        listOf(
            PerkSpell(168, 1, "Smite"),
            PerkBaseStat(169, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkSpell(170, 1, "Consecrate"),
            PerkBaseStat(171, 1, StatType.WISDOM, maxAllocatedPoints = 3),
            PerkBaseStat(172, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkSpell(173, 1, "Salvation"),
            PerkBaseStat(174, 1, StatType.WISDOM, maxAllocatedPoints = 3),
            PerkBaseStat(175, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkBaseStat(177, 1, StatType.WISDOM, maxAllocatedPoints = 3),
            PerkSpell(176, 1, "Sacred Wings"),
            PerkBaseStat(178, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkSpell(179, 1, "Blessed Blade"),
        )
}
