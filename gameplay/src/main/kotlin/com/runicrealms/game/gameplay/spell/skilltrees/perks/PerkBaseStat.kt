package com.runicrealms.game.gameplay.spell.skilltrees.perks

import com.runicrealms.game.common.StatType
import java.util.UUID

/**
 * A [Perk] that grants a flat stat bonus on purchase.
 *
 * TODO: Implement [applyBonus] once StatAPI is migrated. Currently a stub. See
 *   SPELL_MIGRATION.md #2.
 */
class PerkBaseStat(
    perkId: Int,
    cost: Int,
    val stat: StatType,
    val bonusAmount: Int = DEFAULT_BONUS,
) : Perk(perkId, cost, maxAllocatedPoints = 1) {

    /**
     * Applies the stat bonus to the player's stat pool.
     *
     * TODO: Call StatAPI.addBonus(uuid, stat, bonusAmount) once StatAPI is migrated.
     */
    fun applyBonus(uuid: UUID) {
        // TODO (SPELL_MIGRATION.md #2): StatAPI not yet migrated. Stub: no bonus applied.
    }

    companion object {
        const val DEFAULT_BONUS = 2
    }
}
