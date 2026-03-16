package com.runicrealms.game.gameplay.spell.api

import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import java.util.UUID
import org.bukkit.entity.LivingEntity

/** Public API for managing hard crowd-control status effects. */
interface StatusEffectAPI {
    fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
        applier: LivingEntity? = null,
    )

    fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
    )

    /** Removes all debuffs from the given player (cleanse). */
    fun cleanse(uuid: UUID)

    /** Removes all buffs from the given player (purge). */
    fun purge(uuid: UUID)

    fun hasStatusEffect(uuid: UUID, effect: RunicStatusEffect): Boolean

    fun removeStatusEffect(uuid: UUID, statusEffect: RunicStatusEffect): Boolean

    /** Returns the remaining duration in seconds, or 0.0 if not active. */
    fun getStatusEffectDuration(uuid: UUID, effect: RunicStatusEffect): Double
}
