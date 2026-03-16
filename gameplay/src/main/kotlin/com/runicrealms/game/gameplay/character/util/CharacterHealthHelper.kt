package com.runicrealms.game.gameplay.character.util

import com.google.inject.Inject
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import kotlin.math.pow
import org.bukkit.attribute.Attribute

/**
 * This class controls the changing of the player's health, as well as the way their hearts are
 * displayed.
 *
 * @author Skyfallin_
 */
class CharacterHealthHelper
@Inject
constructor(private val characterEquipmentCacheRegistry: CharacterEquipmentCacheRegistry) {

    companion object {
        const val BASE_HEALTH = 200
        const val HEART_AMOUNT = 20
    }

    @Inject private lateinit var characterLevelHelper: CharacterLevelHelper

    /** Sets a character's max health MUST BE CALLED SYNC */
    fun setCharacterMaxHealth(character: GameCharacter) {
        // Grab the player's new info
        val classType = character.withSyncCharacterData { traits.classType }
        val player = character.bukkitPlayer

        // For new players (ANY means no class selected yet)
        val characterLevel = player.level
        val hpPerLevel = characterLevelHelper.determineHealthLvByClass(classType)
        val coefficient = CharacterLevelHelper.HEALTH_LEVEL_COEFFICIENT

        val total =
            (BASE_HEALTH +
                    (coefficient * characterLevel.toDouble().pow(2.0)) +
                    (hpPerLevel * characterLevel) +
                    (characterEquipmentCacheRegistry.getAddedCharacterStats(character)?.health
                        ?: 0))
                .toInt()

        player.getAttribute(Attribute.MAX_HEALTH)!!.baseValue = total.toDouble()
        player.healthScale = HEART_AMOUNT.toDouble()
    }
}
