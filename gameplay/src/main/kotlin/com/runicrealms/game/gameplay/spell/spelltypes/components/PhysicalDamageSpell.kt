package com.runicrealms.game.gameplay.spell.spelltypes.components

/** A spell that deals physical damage. Scaling is applied by SpellScalingListener. */
interface PhysicalDamageSpell {
    var physicalDamage: Double
    var physicalDamagePerLevel: Double
}
