package com.runicrealms.game.items.character

interface StatsModifier {

    /** Gets the AddedStats that we will combine with our current stats. */
    fun getChanges(currentStats: AddedStats): AddedStats
}
