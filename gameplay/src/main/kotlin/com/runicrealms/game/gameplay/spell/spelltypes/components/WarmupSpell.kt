package com.runicrealms.game.gameplay.spell.spelltypes.components

/** A spell with a cast time / channel delay before its effect fires (in seconds). */
interface WarmupSpell {
    var warmupSeconds: Double
}
