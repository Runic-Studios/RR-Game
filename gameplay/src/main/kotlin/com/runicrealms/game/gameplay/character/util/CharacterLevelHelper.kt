package com.runicrealms.game.gameplay.character.util

import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.common.util.sendCenteredMessage
import com.runicrealms.game.data.extension.getInfo
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.gameplay.player.RegenManager
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.time.Duration
import kotlin.math.cbrt
import kotlin.math.pow
import net.kyori.adventure.title.Title

class CharacterLevelHelper @Inject constructor(private val regenManager: RegenManager) {

    companion object {
        const val MAX_LEVEL: Int = 60

        /*
        Class-specific level coefficients
         */
        const val ARCHER_HP_LV: Int = 6
        const val CLERIC_HP_LV: Int = 6
        const val MAGE_HP_LV: Int = 4
        const val ROGUE_HP_LV: Int = 8
        const val WARRIOR_HP_LV: Int = 12

        const val HEALTH_LEVEL_COEFFICIENT: Double = 0.2
    }

    /**
     * Here is our exp curve! At level 50, the player is ~ halfway to max, w/ 997,500 At level 60,
     * the player needs 1,647,000 total exp
     *
     * @param currentLv the current level of the player
     * @return the experience they've earned at that level
     */
    fun calculateTotalExp(currentLv: Int): Int {
        val cubed = (currentLv + 5).toDouble().pow(3.0).toInt()
        return ((53 * cubed) / 5) - 1325
    }

    /**
     * This method takes in an experience amount and returns the level which corresponds to that
     * amount. i.e., passing 997,500 will return level 50.
     *
     * @param experience the experience of the player
     */
    fun calculateExpectedLv(experience: Long): Int {
        return cbrt((((5 * experience) + 6625.0) / 53)).toInt() - 5
    }

    /**
     * Called when a player earns experience towards their combat class MUST BE CALLED SYNC
     *
     * @param player who earned the exp
     * @param expGained the amount of exp earned
     */
    fun giveExperience(character: GameCharacter, expGained: Int) {
        val player = character.bukkitPlayer
        var currentLevel = player.level
        if (currentLevel >= MAX_LEVEL) return
        character.withSyncCharacterData {
            val currentExp = traits.data.exp + expGained

            // If the player's actual level is incorrect based on their total exp, adjust level
            if (calculateExpectedLv(currentExp) != currentLevel) {
                player.sendMessage("\n")
                sendLevelMessage(character, calculateExpectedLv(currentExp))
                player.sendMessage("\n")
                player.level = calculateExpectedLv(currentExp)
                currentLevel = calculateExpectedLv(currentExp)
            }

            val totalExpAtLevel = calculateTotalExp(currentLevel)
            val totalExpToLevel = calculateTotalExp(currentLevel + 1)
            var proportion =
                (currentExp - totalExpAtLevel).toDouble() / (totalExpToLevel - totalExpAtLevel)
            if (currentLevel == MAX_LEVEL) {
                player.exp = 0f
            }
            if (proportion < 0) {
                proportion = 0.0
            }

            traits.data.exp = currentExp
            traits.data.level = currentLevel
            player.exp = proportion.toFloat()
            stageChanges(traits)
        }
    }

    /**
     * When the player earns a level, send them a message! MUST BE CALLED SYNC
     *
     * @param player to receive message
     * @param classLv the level they reached
     */
    private fun sendLevelMessage(character: GameCharacter, classLv: Int) {
        character.withSyncCharacterData {
            val classType = traits.data.classType
            val className = classType.getInfo().name
            val player = character.bukkitPlayer
            player.showTitle(
                Title.title(
                    "&aLevel Up!".colorFormat(),
                    "&a$className Level &f$classLv".colorFormat(),
                    Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500),
                    ),
                )
            )
            // save player hp, restore hp.food
            player.sendMessage("\n")
            if (classLv != MAX_LEVEL) sendCenteredMessage(player, "&a&lLEVEL UP!")
            else sendCenteredMessage(player, "&6&lMAX LEVEL REACHED!")
            val gainedHealth =
                calculateHealthAtLevel(classLv, classType) -
                    calculateHealthAtLevel(classLv - 1, classType)
            val gainedMana = regenManager.getManaPerLv(character)
            sendCenteredMessage(player, "&c&l+$gainedHealth❤ &3+$gainedMana✸")
            player.sendMessage("\n")
        }
    }

    /**
     * Calculates the base health of the player based on class and current level
     *
     * @param currentLv their class level
     * @param classType their class name
     * @return the HP they should have based on scaling
     */
    fun calculateHealthAtLevel(currentLv: Int, classType: ClassType): Int {
        val hpPerLevel = determineHealthLvByClass(classType)
        return (CharacterHealthHelper.BASE_HEALTH +
                (HEALTH_LEVEL_COEFFICIENT * currentLv.toDouble().pow(2.0)) +
                (hpPerLevel * currentLv))
            .toInt()
    }

    /**
     * May return either the scaling coefficient or linear hp-per-level of class based on boolean
     * flag value
     *
     * @param classType type of class
     * @return um can return either dis might be bad but to lazy to write two methods
     */
    fun determineHealthLvByClass(classType: ClassType): Double {
        return when (classType) {
            ClassType.ARCHER -> ARCHER_HP_LV.toDouble()
            ClassType.CLERIC -> CLERIC_HP_LV.toDouble()
            ClassType.MAGE -> MAGE_HP_LV.toDouble()
            ClassType.ROGUE -> ROGUE_HP_LV.toDouble()
            ClassType.WARRIOR -> WARRIOR_HP_LV.toDouble()
            else -> throw IllegalStateException("Unexpected value: $classType")
        }
    }
}
