package com.runicrealms.game.items.character

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType

class FlatStatsModifier
@AssistedInject
constructor(
    @Assisted stats: MutableMap<StatType, Int>,
    @Assisted itemPerks: MutableCollection<ItemData.Perk>?,
    @Assisted health: Int,
    addStatsFactory: AddedStats.Factory,
) : StatsModifier {

    interface Factory {
        fun create(
            stats: MutableMap<StatType, Int>,
            itemPerks: MutableCollection<ItemData.Perk>?,
            health: Int,
        ): FlatStatsModifier
    }

    private val stats = addStatsFactory.create(stats, itemPerks, health)

    override fun getChanges(currentStats: AddedStats): AddedStats {
        return stats
    }
}
