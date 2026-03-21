package com.runicrealms.game.gameplay.spell.effect.cleric

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.StackEffect
import com.runicrealms.game.gameplay.spell.effect.StackHologram
import com.runicrealms.game.gameplay.spell.spellutil.particles.HelixParticleFrame
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * StackEffect buff on caster. Plays HelixParticleFrame (FIREWORKS_SPARK) when at or above
 * [stackThreshold] stacks. On expiry (or stack drain), the buff fires.
 */
class RadiantFireEffect(
    override val caster: Player,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val maxStacks: Int,
    private val stackThreshold: Int,
    private val spellEffectAPI: SpellEffectAPI,
) : StackEffect {

    override val recipient: LivingEntity = caster
    override val effectType = SpellEffectType.RADIANT_FIRE
    override val isBuff = true
    override val stacks = AtomicInteger(0)
    override val tickInterval = 20
    override val stackHologram: StackHologram =
        StackHologram(
            SpellEffectType.RADIANT_FIRE,
            caster.location,
            "stacks_RADIANT_FIRE_${caster.uniqueId}_${caster.uniqueId}",
            setOf(caster),
        )

    private var nextTick = 0
    private val helix = HelixParticleFrame()

    override fun setNextTickCounter(counter: Int) {
        nextTick = counter
    }

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        stackHologram.showHologram(caster.location, stacks.get())
        executeSpellEffect()
        initializeNextTick(globalCounter)
    }

    override fun executeSpellEffect() {
        if (stacks.get() >= stackThreshold) {
            helix.playParticle(caster, Particle.FIREWORK, caster.location, 0.15)
        }
    }

    fun increment(): Boolean {
        if (stacks.get() >= maxStacks) return false
        stacks.incrementAndGet()
        stackHologram.showHologram(caster.location, stacks.get())
        return true
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun onExpire() {
        stackHologram.remove()
    }

    override fun cancel() {
        onExpire()
    }
}
