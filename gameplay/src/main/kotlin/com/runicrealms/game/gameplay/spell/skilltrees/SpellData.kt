package com.runicrealms.game.gameplay.spell.skilltrees

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.data.model.CharacterSpells

/**
 * Runtime mirror of [CharacterSpells] with the 4 spell slot assignments.
 *
 * Default spells are granted on first character creation and reset on skill tree wipe.
 */
data class SpellData(
    var spellHotbarOne: String,
    var spellLeftClick: String,
    var spellRightClick: String,
    var spellSwapHands: String,
) {

    companion object {
        private const val RAPID_FIRE = "Rapid Fire"
        private const val SACRED_SPRING = "Sacred Spring"
        private const val FIREBALL = "Fireball"
        private const val DASH = "Dash"
        private const val SLAM = "Slam"

        fun defaultForClass(classType: ClassType): SpellData =
            when (classType) {
                ClassType.ARCHER -> SpellData(RAPID_FIRE, RAPID_FIRE, RAPID_FIRE, RAPID_FIRE)
                ClassType.CLERIC ->
                    SpellData(SACRED_SPRING, SACRED_SPRING, SACRED_SPRING, SACRED_SPRING)
                ClassType.MAGE -> SpellData(FIREBALL, FIREBALL, FIREBALL, FIREBALL)
                ClassType.ROGUE -> SpellData(DASH, DASH, DASH, DASH)
                ClassType.WARRIOR -> SpellData(SLAM, SLAM, SLAM, SLAM)
                ClassType.ANY -> SpellData("", "", "", "")
            }

        fun fromCharacterSpells(characterSpells: CharacterSpells): SpellData =
            SpellData(
                characterSpells.spellOneID,
                characterSpells.spellTwoID,
                characterSpells.spellThreeID,
                characterSpells.spellFourID,
            )
    }

    fun toCharacterSpells(): CharacterSpells =
        CharacterSpells(spellHotbarOne, spellLeftClick, spellRightClick, spellSwapHands)

    /** Returns the spell name assigned to the given slot index (0-based). */
    fun getSpellForSlotIndex(index: Int): String? =
        when (index) {
            0 -> spellHotbarOne.takeIf { it.isNotBlank() }
            1 -> spellLeftClick.takeIf { it.isNotBlank() }
            2 -> spellRightClick.takeIf { it.isNotBlank() }
            3 -> spellSwapHands.takeIf { it.isNotBlank() }
            else -> null
        }
}
