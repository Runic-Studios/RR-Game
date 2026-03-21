package com.runicrealms.game.gameplay.spell.api

import java.util.UUID

/** Public API for querying player skill tree and spell data. */
interface SkillTreeAPI {
    fun getAvailableSkillPoints(uuid: UUID, slot: Int): Int

    fun getPassives(uuid: UUID): Set<String>

    fun getSpentPoints(uuid: UUID, slot: Int): Int

    fun hasPassiveFromSkillTree(uuid: UUID, passive: String): Boolean

    /** Returns the spell name assigned to the given slot index (0-3), or null if unset. */
    fun getPlayerSpellName(uuid: UUID, slot: Int, spellSlotIndex: Int): String?
}
