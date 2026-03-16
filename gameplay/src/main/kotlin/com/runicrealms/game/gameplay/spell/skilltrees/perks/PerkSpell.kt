package com.runicrealms.game.gameplay.spell.skilltrees.perks

/**
 * A [Perk] that unlocks a spell. [spellName] matches the
 * [com.runicrealms.game.gameplay.spell.spelltypes.Spell.name] (case-insensitive).
 */
class PerkSpell(perkId: Int, cost: Int, val spellName: String) :
    Perk(perkId, cost, maxAllocatedPoints = 1)
