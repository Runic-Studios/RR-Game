package com.runicrealms.game.gameplay.spell.skilltrees.util

import com.runicrealms.game.common.StatType
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell

/** Defines the perk lists for all three Rogue sub-classes. */
object RogueTreeUtil {

    val DEFAULT_ROGUE_SPELL_PERK = PerkSpell(perkId = 998, cost = 0, spellName = "Dash")

    fun nightcrawlerPerkList(): List<Perk> =
        listOf(
            PerkSpell(108, 1, "Twin Fangs"),
            PerkBaseStat(109, 1, StatType.STRENGTH, maxAllocatedPoints = 5),
            PerkSpell(110, 1, "Backstab"),
            PerkBaseStat(111, 1, StatType.DEXTERITY, maxAllocatedPoints = 3),
            PerkBaseStat(112, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkSpell(113, 1, "Cocoon"),
            PerkBaseStat(114, 1, StatType.DEXTERITY, maxAllocatedPoints = 3),
            PerkBaseStat(115, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkBaseStat(117, 1, StatType.DEXTERITY, maxAllocatedPoints = 5),
            PerkSpell(116, 1, "Unseen"),
            PerkBaseStat(118, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkSpell(119, 1, "From The Shadows"),
        )

    fun witchHunterPerkList(): List<Perk> =
        listOf(
            PerkSpell(120, 1, "Silver Bolt"),
            PerkBaseStat(121, 1, StatType.DEXTERITY, maxAllocatedPoints = 5),
            PerkSpell(122, 1, "Castigate"),
            PerkBaseStat(123, 1, StatType.INTELLIGENCE, maxAllocatedPoints = 3),
            PerkBaseStat(124, 1, StatType.DEXTERITY, maxAllocatedPoints = 3),
            PerkSpell(125, 1, "Flay"),
            PerkBaseStat(126, 1, StatType.INTELLIGENCE, maxAllocatedPoints = 3),
            PerkBaseStat(127, 1, StatType.DEXTERITY, maxAllocatedPoints = 3),
            PerkBaseStat(129, 1, StatType.INTELLIGENCE, maxAllocatedPoints = 5),
            PerkSpell(128, 1, "Warding Glyph"),
            PerkBaseStat(130, 1, StatType.DEXTERITY, maxAllocatedPoints = 3),
            PerkSpell(131, 1, "Hereticize"),
        )

    fun corsairPerkList(): List<Perk> =
        listOf(
            PerkSpell(132, 1, "Cannonfire"),
            PerkBaseStat(133, 1, StatType.STRENGTH, maxAllocatedPoints = 5),
            PerkSpell(134, 1, "Scurvy"),
            PerkBaseStat(135, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkBaseStat(136, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkSpell(137, 1, "Whirlpool"),
            PerkBaseStat(138, 1, StatType.VITALITY, maxAllocatedPoints = 3),
            PerkBaseStat(139, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkBaseStat(141, 1, StatType.VITALITY, maxAllocatedPoints = 5),
            PerkSpell(140, 1, "Harpoon"),
            PerkBaseStat(142, 1, StatType.STRENGTH, maxAllocatedPoints = 3),
            PerkSpell(143, 1, "Call Of The Deep"),
        )
}
