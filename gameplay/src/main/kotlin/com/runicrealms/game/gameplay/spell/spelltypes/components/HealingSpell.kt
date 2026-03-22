package com.runicrealms.game.gameplay.spell.spelltypes.components

/** A spell that heals players. Scaling is applied by SpellScalingListener. */
interface HealingSpell {
    var healAmount: Double
    var healPerLevel: Double
}
