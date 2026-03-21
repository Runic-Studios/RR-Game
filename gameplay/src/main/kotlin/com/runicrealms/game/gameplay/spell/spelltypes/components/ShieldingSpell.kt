package com.runicrealms.game.gameplay.spell.spelltypes.components

/** A spell that applies a shield. Scaling is applied by SpellScalingListener. */
interface ShieldingSpell {
    var shieldAmount: Double
    var shieldPerLevel: Double
}
