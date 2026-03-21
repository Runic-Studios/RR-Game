package com.runicrealms.game.gameplay.spell.spellutil

import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * Knockback utility. Skips entities identified as bosses (MythicMobs faction "boss"). Uses
 * MythicMobs 5.6.1 API for faction checks.
 */
object KnockbackUtil {

    const val MELEE_STRENGTH = 0.65
    const val RANGED_STRENGTH = 0.5
    const val MAX_VELOCITY = 0.65

    /** Applies melee knockback from [source] to [target]. Skips boss mobs. */
    fun knockbackMeleePlayer(source: Player, target: Entity) {
        if (isBoss(target)) return
        applyKnockback(source, target, MELEE_STRENGTH)
    }

    /** Applies ranged knockback from [source] to [target]. Skips boss mobs. */
    fun knockbackRangedPlayer(source: Player, target: Entity) {
        if (isBoss(target)) return
        applyKnockback(source, target, RANGED_STRENGTH)
    }

    /** Applies knockback from [source] mob to [target]. Skips boss mobs. */
    fun knockBackMob(source: Entity, target: Entity, strength: Double) {
        if (isBoss(target)) return
        applyKnockback(source, target, strength)
    }

    /** Applies custom-direction knockback with [strength]. Skips boss mobs. */
    fun knockBackCustom(target: Entity, direction: Vector, strength: Double) {
        if (isBoss(target)) return
        if (target !is LivingEntity) return
        val velocity = direction.normalize().multiply(strength)
        velocity.y = velocity.y.coerceAtMost(MAX_VELOCITY)
        target.velocity = velocity
    }

    private fun applyKnockback(source: Entity, target: Entity, strength: Double) {
        if (target !is LivingEntity) return
        val direction = target.location.toVector().subtract(source.location.toVector()).normalize()
        direction.y = 0.15
        val velocity = direction.multiply(strength)
        velocity.x = velocity.x.coerceAtMost(MAX_VELOCITY)
        velocity.z = velocity.z.coerceAtMost(MAX_VELOCITY)
        target.velocity = velocity
    }

    /** Returns true if [entity] is a MythicMob with faction "boss". */
    private fun isBoss(entity: Entity): Boolean {
        return MythicBukkit.inst()
            .mobManager
            .getActiveMob(entity.uniqueId)
            .map { activeMob ->
                activeMob.hasFaction() && activeMob.faction.equals("boss", ignoreCase = true)
            }
            .orElse(false)
    }
}
