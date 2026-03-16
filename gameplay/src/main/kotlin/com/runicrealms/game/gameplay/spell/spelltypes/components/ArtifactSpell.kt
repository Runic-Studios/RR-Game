package com.runicrealms.game.gameplay.spell.spelltypes.components

/** Marks a spell as tied to an artifact item. */
interface ArtifactSpell {
    val artifactId: String
    val chance: Double
}
