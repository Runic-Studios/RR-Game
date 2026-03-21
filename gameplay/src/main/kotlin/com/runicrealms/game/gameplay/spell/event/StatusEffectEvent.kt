package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired whenever a [RunicStatusEffect] is applied to an entity. Cancel to prevent the effect from
 * being applied.
 */
class StatusEffectEvent(
    val entity: LivingEntity,
    val statusEffect: RunicStatusEffect,
    val durationInSeconds: Double,
    val displayMessage: Boolean,
    val applier: LivingEntity? = null,
    var playSound: Boolean = true,
) : Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
