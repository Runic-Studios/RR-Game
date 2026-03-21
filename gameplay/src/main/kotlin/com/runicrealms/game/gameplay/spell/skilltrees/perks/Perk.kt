package com.runicrealms.game.gameplay.spell.skilltrees.perks

/**
 * Abstract base for a single skill tree node. A perk has:
 * - A unique [perkId] for persistence
 * - A [cost] in skill points
 * - [currentlyAllocatedPoints] / [maxAllocatedPoints] tracking
 */
abstract class Perk(
    val perkId: Int,
    val cost: Int,
    val maxAllocatedPoints: Int,
    var currentlyAllocatedPoints: Int = 0,
) {
    fun isMaxed(): Boolean = currentlyAllocatedPoints >= maxAllocatedPoints

    fun isPurchased(): Boolean = currentlyAllocatedPoints > 0

    /** Increments allocation by 1 if below max. Returns true if successful. */
    fun allocate(): Boolean {
        if (isMaxed()) return false
        currentlyAllocatedPoints++
        return true
    }

    /** Resets allocation to 0. */
    fun reset() {
        currentlyAllocatedPoints = 0
    }
}
