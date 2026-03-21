package com.runicrealms.game.gameplay.spell.spelltypes.components

/** A spell that deals magic (spell) damage. Scaling is applied by SpellScalingListener. */
interface MagicDamageSpell {
    var magicDamage: Double
    var magicDamagePerLevel: Double
}
