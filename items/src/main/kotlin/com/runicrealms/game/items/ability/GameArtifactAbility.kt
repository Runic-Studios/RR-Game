package com.runicrealms.game.items.ability

import com.runicrealms.game.items.util.GameArtifactAbilityTrigger

/**
 * Represents an artifact ability that can be triggered by items.
 *
 * @param identifier the unique identifier, e.g. "adrenaline-rush"
 * @param name the display name, e.g. "Adrenaline Rush"
 * @param description a description of the passive effect
 * @param trigger the type of trigger for the ability
 */
data class GameArtifactAbility(
    val identifier: String,
    val name: String,
    val description: String,
    val trigger: GameArtifactAbilityTrigger,
)
