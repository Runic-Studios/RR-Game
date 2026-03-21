package com.runicrealms.game.gameplay.spell.effect.warrior

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.StackEffect
import com.runicrealms.game.gameplay.spell.effect.StackHologram
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * StackEffect buff on caster. Stacks drain to 0 after [stackDuration] seconds (no decrement by 1,
 * the task fully cancels the stacks when it fires). Provides a visual indicator via
 * [StackHologram].
 */
class BlessedBladeEffect(
    override val caster: Player,
    override val duration: Double,
    private val stackDuration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : StackEffect {

    override val recipient: LivingEntity = caster
    override val effectType = SpellEffectType.BLESSED_BLADE
    override val isBuff = true
    override val stacks = AtomicInteger(0)
    override val tickInterval = 20
    override val startTime: Long = System.currentTimeMillis()
    override val stackHologram: StackHologram =
        StackHologram(
            SpellEffectType.BLESSED_BLADE,
            caster.location,
            "stacks_BLESSED_BLADE_${caster.uniqueId}_${caster.uniqueId}",
            setOf(caster),
        )

    private var nextTick = 0

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
        caster.world.spawnParticle(
            Particle.HAPPY_VILLAGER,
            caster.location.add(0.0, 1.0, 0.0),
            5,
            0.3,
            0.5,
            0.3,
        )
    }

    /** Increments stacks and resets the decay timer. */
    fun increment(location: Location, newStacks: Int) {
        stacks.set(newStacks)
        stackHologram.showHologram(location, newStacks)
    }

    /** Resets stacks to 0 (called when the decay timer fires). */
    fun decrement(location: Location, stackCount: Int) {
        stacks.set(stackCount)
        stackHologram.showHologram(location, stackCount)
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
