package com.runicrealms.game.gameplay.spell.spelltypes

/** Indicates which item context triggered the spell. */
enum class SpellItemType(val displayName: String, val slot: Int) {
    ARTIFACT("Artifact", -1),
    RUNE("Rune", 0),
}
