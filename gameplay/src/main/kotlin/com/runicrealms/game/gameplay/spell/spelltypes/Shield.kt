package com.runicrealms.game.gameplay.spell.spelltypes

import java.util.UUID

/** Holds the state of an active spell shield on a player. */
class Shield(
    var amount: Double,
    var startTime: Long,
    sourceUuid: UUID,
    val durationMs: Long = DEFAULT_DURATION_MS,
) {
    val sources: MutableSet<UUID> = mutableSetOf(sourceUuid)

    fun isExpired(): Boolean = System.currentTimeMillis() > startTime + durationMs

    /** Resets the start time so the shield lasts another [durationMs] milliseconds. */
    fun refresh() {
        startTime = System.currentTimeMillis()
    }

    fun addSource(sourceUuid: UUID) {
        sources.add(sourceUuid)
    }

    companion object {
        const val DEFAULT_DURATION_MS = 10_000L
    }
}
