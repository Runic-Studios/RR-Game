package com.runicrealms.game.gameplay.spell.spelltypes

/** Controls which input activates the initial spell trigger step. */
enum class SpellTriggerType {
    /** Archer: left-click activates the spell cast UI. */
    ARCHER,

    /** All other classes: right-click activates the spell cast UI. */
    DEFAULT,
}
