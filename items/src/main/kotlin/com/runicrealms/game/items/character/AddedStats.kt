package com.runicrealms.game.items.character

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.items.config.perk.GameItemPerkTemplate
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import java.util.HashMap
import javax.annotation.Nullable

class AddedStats
@AssistedInject
constructor(
    @Assisted val stats: MutableMap<StatType, Int>,
    @Nullable @Assisted perks: MutableCollection<ItemData.Perk>?,
    @Assisted health: Int,
    private val perkTemplateRegistry: GameItemPerkTemplateRegistry,
) {

    interface Factory {
        fun create(
            stats: MutableMap<StatType, Int>,
            perks: MutableCollection<ItemData.Perk>?,
            health: Int,
        ): AddedStats
    }

    var perks = perks
        private set

    var health = health
        private set

    @Synchronized
    internal fun addPerk(perk: ItemData.Perk) {
        if (perks == null) {
            perks = mutableSetOf()
        }
        perks!!.add(perk)
    }

    fun hasItemPerks() = !perks.isNullOrEmpty()

    /** Adds the stats of another AddedStats object to the stats of this object */
    @Synchronized
    fun combine(moreStats: AddedStats) {
        health += moreStats.health
        for (stat in moreStats.stats.keys) {
            stats[stat] = stats.getOrDefault(stat, 0) + moreStats.stats[stat]!!
        }
        if (perks != null || moreStats.perks != null) {
            val perks = HashMap<GameItemPerkTemplate, Int>()
            if (this.perks != null) {
                for (perk in this.perks!!) {
                    val perkType =
                        perkTemplateRegistry.getPerkTemplate(perk.perkID)
                            ?: throw IllegalArgumentException(
                                "Cannot find perk with type ${perk.perkID}"
                            )
                    perks[perkType] = perks.getOrDefault(perkType, 0) + perk.stacks
                }
            }
            if (moreStats.perks != null) {
                for (perk in moreStats.perks!!) {
                    val perkType =
                        perkTemplateRegistry.getPerkTemplate(perk.perkID)
                            ?: throw IllegalArgumentException(
                                "Cannot find perk with type ${perk.perkID}"
                            )
                    perks[perkType] = perks.getOrDefault(perkType, 0) + perk.stacks
                }
            }
            this.perks?.clear()
            if (this.perks == null) {
                this.perks = mutableSetOf()
            }
            for ((perkType, stacks) in perks) {
                val perk =
                    ItemData.Perk.newBuilder()
                        .setPerkID(perkType.identifier)
                        .setStacks(stacks)
                        .build()
                this.perks!!.add(perk)
            }
        }
    }
}
