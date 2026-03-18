package com.runicrealms.game.items.ability

/**
 * Registry for artifact abilities. Other systems (spells, gameplay) register abilities here
 * and look them up by ID.
 */
class GameAbilityManager {

    private val abilities = HashMap<String, GameArtifactAbility>()

    fun registerAbility(ability: GameArtifactAbility) {
        abilities[ability.identifier] = ability
    }

    fun registerAbilities(abilities: Map<String, GameArtifactAbility>) {
        this.abilities.putAll(abilities)
    }

    fun getAbility(identifier: String): GameArtifactAbility? {
        return abilities[identifier]
    }

    fun getAbilities(): Collection<GameArtifactAbility> = abilities.values
}
