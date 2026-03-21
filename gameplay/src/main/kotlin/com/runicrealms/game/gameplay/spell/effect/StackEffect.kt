package com.runicrealms.game.gameplay.spell.effect

import java.util.concurrent.atomic.AtomicInteger

/**
 * Extension of [SpellEffect] for effects that accumulate stacks (e.g. Bleed, Charged, Sundered). A
 * [StackHologram] is shown only to the caster and recipient.
 */
interface StackEffect : SpellEffect {
    val stacks: AtomicInteger
    val tickInterval: Int
    val stackHologram: StackHologram?

    /** Records the next tick counter at which this effect should fire. */
    fun setNextTickCounter(counter: Int)

    /** Advances [setNextTickCounter] by [tickInterval] based on [globalCounter]. */
    fun initializeNextTick(globalCounter: Int) {
        setNextTickCounter(globalCounter + tickInterval)
    }
}
