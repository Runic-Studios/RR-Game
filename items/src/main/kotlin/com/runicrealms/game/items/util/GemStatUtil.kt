package com.runicrealms.game.items.util

import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import io.netty.util.internal.ThreadLocalRandom

object GemStatUtil {
    // Maps gem tier to a collection of options including the main stat value (pair key) and the sub
    // stat values (pair entry)
    private val GEM_STAT_OPTIONS: MutableMap<Int, List<Pair<Int, IntArray>>> = HashMap()

    // Maps a gem tier to the number of gem slots (slots) it consumes
    private val GEM_TIER_SLOTS: MutableMap<Int, Int>

    init {
        val tierOne: MutableList<Pair<Int, IntArray>> = ArrayList(1)
        tierOne.add(Pair(1, IntArray(0)))
        GEM_STAT_OPTIONS[1] = tierOne

        val tierTwo: MutableList<Pair<Int, IntArray>> = ArrayList(1)
        tierTwo.add(Pair(3, IntArray(0)))
        GEM_STAT_OPTIONS[2] = tierTwo

        val tierThree: MutableList<Pair<Int, IntArray>> = ArrayList(2)
        tierThree.add(Pair(4, IntArray(0)))
        tierThree.add(Pair(3, intArrayOf(1, 1)))
        GEM_STAT_OPTIONS[3] = tierThree

        val tierFour: MutableList<Pair<Int, IntArray>> = ArrayList(3)
        tierFour.add(Pair(8, IntArray(0)))
        tierFour.add(Pair(7, intArrayOf(2)))
        tierFour.add(Pair(7, intArrayOf(1, 1)))
        GEM_STAT_OPTIONS[4] = tierFour

        val tierFive: MutableList<Pair<Int, IntArray>> = ArrayList(4)
        tierFive.add(Pair(10, IntArray(0)))
        tierFive.add(Pair(9, intArrayOf(3)))
        tierFive.add(Pair(9, intArrayOf(2, 2)))
        GEM_STAT_OPTIONS[5] = tierFive

        GEM_TIER_SLOTS = HashMap()
        GEM_TIER_SLOTS[1] = 1
        GEM_TIER_SLOTS[2] = 2
        GEM_TIER_SLOTS[3] = 2
        GEM_TIER_SLOTS[4] = 3
        GEM_TIER_SLOTS[5] = 3
    }

    /**
     * Generates randomized bonuses that a gem will give based upon its tier and main stat. Tier
     * should be a number between 1 and 5, **0 isn't a tier**.
     *
     * @param tier Tier of the gem
     * @param mainStat Main stat of the gem
     * @return Sorted map of the stats
     */
    fun generateGemBonuses(tier: Int, mainStat: StatType): List<ItemData.StaticStat> {
        require(!(tier < 0 || tier > GEM_STAT_OPTIONS.size)) {
            "Tier $tier does not exist for gem bonuses."
        }
        val stats = mutableListOf<ItemData.StaticStat>()
        val tierOptions = GEM_STAT_OPTIONS[tier]!!
        val index = ThreadLocalRandom.current().nextInt(tierOptions.size)
        val selectedOption = tierOptions[index]
        val mainStaticStat =
            ItemData.StaticStat.newBuilder()
                .setType(mainStat)
                .setAmount(selectedOption.first)
                .build()
        stats.add(mainStaticStat)
        val statsToChoose = ArrayList(StatType.entries)
        statsToChoose.remove(mainStat)
        for (subStatBonus in selectedOption.second) {
            val randomSubStatIndex = ThreadLocalRandom.current().nextInt(statsToChoose.size)
            val selectedSubStat = statsToChoose[randomSubStatIndex]
            val staticStat =
                ItemData.StaticStat.newBuilder()
                    .setType(selectedSubStat)
                    .setAmount(subStatBonus)
                    .build()
            stats.add(staticStat)
            statsToChoose.removeAt(randomSubStatIndex)
        }
        stats.sortBy { it.type.name }
        return stats
    }

    /**
     * Gets the number of gem slots (slots) that the specified gem will consume.
     *
     * @param tier Gem tier
     * @return Number of slots
     */
    fun getGemSlots(tier: Int): Int {
        return GEM_TIER_SLOTS[tier]!!
    }
}
