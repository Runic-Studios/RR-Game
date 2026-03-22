package com.runicrealms.game.gameplay.spell.skilltrees.perks

import com.runicrealms.game.common.StatType

/**
 * A [Perk] that grants a flat stat bonus on purchase.
 *
 * Stat application is handled by [com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager]
 * via [com.runicrealms.game.gameplay.player.stat.StatManager.addBaseStatBonus] when the perk is
 * allocated, and on character load by [com.runicrealms.game.gameplay.player.stat.StatManager].
 */
class PerkBaseStat(
    perkId: Int,
    cost: Int,
    val stat: StatType,
    val bonusAmount: Int = DEFAULT_BONUS,
    maxAllocatedPoints: Int = 1,
) : Perk(perkId, cost, maxAllocatedPoints) {

    companion object {
        const val DEFAULT_BONUS = 2
    }
}
