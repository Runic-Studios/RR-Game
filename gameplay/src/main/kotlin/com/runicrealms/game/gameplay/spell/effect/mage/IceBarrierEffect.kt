package com.runicrealms.game.gameplay.spell.effect.mage

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
 * StackEffect buff on caster. Plays HelixParticleFrame (BLOCK_CRACK PACKED_ICE) every 2 seconds.
 * Stack count is mutable via [setMaxStacks] (used by Glacier passive). Damage reduction:
 * [baseValue] + [multiplier] * statValue percent per stack.
 */
class IceBarrierEffect(
    override val caster: Player,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private var maxStacks: Int,
    private val spellEffectAPI: SpellEffectAPI,
) : StackEffect {

    override val recipient: LivingEntity = caster
    override val effectType = SpellEffectType.ICE_BARRIER
    override val isBuff = true
    override val stacks = AtomicInteger(0)
    override val tickInterval = 40
    override val stackHologram: StackHologram =
        StackHologram(
            SpellEffectType.ICE_BARRIER,
            caster.location,
            "stacks_ICE_BARRIER_${caster.uniqueId}_${caster.uniqueId}",
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
        helix.playParticle(caster, Particle.BLOCK_CRUMBLE, caster.location, 0.15)
    }

    /** Increments stacks up to [maxStacks]. Returns true if actually added. */
    fun addStack(): Boolean {
        if (stacks.get() >= maxStacks) return false
        stacks.incrementAndGet()
        stackHologram.showHologram(caster.location, stacks.get())
        return true
    }

    /** Removes all stacks (called on LeaveCombatEvent). */
    fun clearStacks() {
        stacks.set(0)
        stackHologram.showHologram(caster.location, 0)
    }

    fun setMaxStacks(maxStacks: Int) {
        this.maxStacks = maxStacks
    }

    fun getMaxStacks(): Int = maxStacks

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun onExpire() {
        stackHologram.remove()
    }

    override fun cancel() {
        clearStacks()
        onExpire()
    }
}
