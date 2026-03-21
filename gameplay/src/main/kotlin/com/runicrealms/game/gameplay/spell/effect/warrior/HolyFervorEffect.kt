package com.runicrealms.game.gameplay.spell.effect.warrior

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * Buff on caster. Draws FIREWORKS_SPARK particles in a wing shape behind the player once per
 * second.
 */
class HolyFervorEffect(
    override val caster: Player,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val recipient: LivingEntity = caster
    override val effectType = SpellEffectType.HOLY_FERVOR
    override val isBuff = true

    private var nextTick = 0

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        val loc = caster.location
        val dir = loc.direction.setY(0).normalize()
        val back = dir.clone().multiply(-1)
        // Wing shape: 15x16 boolean matrix (simplified to two arcs)
        for (i in -7..7) {
            val wingVec = back.clone().add(rotateAroundAxisY(back.clone(), 90.0).multiply(i * 0.1))
            val wingLoc = loc.clone().add(wingVec)
            wingLoc.add(0.0, 0.5 - Math.abs(i) * 0.03, 0.0)
            caster.world.spawnParticle(Particle.FIREWORK, wingLoc, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun cancel() {}

    private fun rotateAroundAxisY(v: Vector, angle: Double): Vector {
        val rad = Math.toRadians(angle)
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val x = v.x
        val z = v.z
        v.x = cos * x - sin * z
        v.z = sin * x + cos * z
        return v
    }
}
