package com.runicrealms.game.items.character

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.StatType
import com.runicrealms.game.data.model.Perk
import javax.annotation.Nullable

class FlatStatsModifier
@AssistedInject
constructor(
    @Assisted stats: MutableMap<StatType, Int>,
    @Nullable @Assisted itemPerks: MutableCollection<Perk>?,
    @Assisted health: Int,
    addStatsFactory: AddedStats.Factory,
) : StatsModifier {

    interface Factory {
        fun create(
            stats: MutableMap<StatType, Int>,
            itemPerks: MutableCollection<Perk>?,
            health: Int,
        ): FlatStatsModifier
    }

    private val stats = addStatsFactory.create(stats, itemPerks, health)

    override fun getChanges(currentStats: AddedStats): AddedStats {
        return stats
    }
}
