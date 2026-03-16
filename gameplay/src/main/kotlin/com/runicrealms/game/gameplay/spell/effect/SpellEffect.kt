package com.runicrealms.game.gameplay.spell.effect

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Contract for all periodic spell effects (Bleed, Chilled, Charged, etc.).
 *
 * Each effect is ticked every [SpellEffectManager.TICK_PERIOD] game ticks by [SpellEffectManager].
 * Expired effects have [onExpire] called, then are removed.
 */
interface SpellEffect {
    val effectType: SpellEffectType
    val caster: Player
    val recipient: LivingEntity
    val isBuff: Boolean
    val startTime: Long
        get() = 0L

    val duration: Double
        get() = 0.0

    fun isActive(): Boolean = (System.currentTimeMillis() - startTime) <= (duration * 1000L)

    /**
     * Called every [SpellEffectManager.TICK_PERIOD] ticks. [globalCounter] is the running tick
     * count.
     */
    fun tick(globalCounter: Int)

    /** Applies the effect's primary gameplay action (damage, heal, visual, etc.). */
    fun executeSpellEffect()

    /** Registers this effect with [SpellEffectManager] via [SpellEffectEvent]. */
    fun initialize() {
        // Implemented in SpellEffectManager via injection; default fires the registration event
    }

    /** Called when this effect expires naturally (after [duration] seconds). */
    fun onExpire() {}

    /** Immediately cancels this effect before its natural expiry. */
    fun cancel()
}
