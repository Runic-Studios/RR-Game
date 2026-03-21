package com.runicrealms.game.gameplay.spell.spelltypes

/** The four input bindings a player can map spells to. */
enum class SpellSlot(val field: String) {
    HOT_BAR_ONE("hotBarOne"),
    LEFT_CLICK("leftClick"),
    RIGHT_CLICK("rightClick"),
    SWAP_HANDS("swapHands"),
}
