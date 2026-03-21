package com.runicrealms.game.gameplay.spell.skilltrees

/** The three skill tree sub-class positions available per character. */
enum class SkillTreePosition(val value: Int) {
    FIRST(1),
    SECOND(2),
    THIRD(3);

    companion object {
        fun fromValue(value: Int): SkillTreePosition =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid SkillTreePosition value: $value")
    }
}
