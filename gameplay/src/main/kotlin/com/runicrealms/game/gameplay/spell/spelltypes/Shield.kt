package com.runicrealms.game.gameplay.spell.spelltypes

import java.util.UUID

/** Holds the state of an active spell shield on a player. */
class Shield(var amount: Double, var startTime: Long, sourceUuid: UUID) {
    val sources: MutableSet<UUID> = mutableSetOf(sourceUuid)

    fun addSource(sourceUuid: UUID) {
        sources.add(sourceUuid)
    }
}
