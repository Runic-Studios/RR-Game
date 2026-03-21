package com.runicrealms.game.gameplay.spell.effect.rogue

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.StackEffect
import com.runicrealms.game.gameplay.spell.effect.StackHologram
import com.runicrealms.game.gameplay.spell.spellutil.particles.Cone
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * StackEffect debuff on recipient from caster. At max stacks, plays Cone effect (REDSTONE,
 * Color.BLUE) once per tick interval.
 */
class SunderedEffect(
    override val caster: Player,
    override val recipient: LivingEntity,
    private val maxStacks: Int,
    override val duration: Double,
    override val startTime: Long = System.currentTimeMillis(),
    private val spellEffectAPI: SpellEffectAPI,
) : StackEffect {

    override val effectType = SpellEffectType.SUNDERED
    override val isBuff = false
    override val stacks = AtomicInteger(0)
    override val tickInterval = 20
    override val stackHologram: StackHologram? =
        if (recipient is Player) {
            StackHologram(
                SpellEffectType.SUNDERED,
                recipient.location,
                "stacks_SUNDERED_${caster.uniqueId}_${recipient.uniqueId}",
                setOf(caster),
            )
        } else null

    private var nextTick = 0

    override fun setNextTickCounter(counter: Int) {
        nextTick = counter
    }

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        stackHologram?.showHologram(recipient.location, stacks.get())
        executeSpellEffect()
        initializeNextTick(globalCounter)
    }

    override fun executeSpellEffect() {
        if (stacks.get() >= maxStacks) {
            Cone.coneEffect(recipient, Particle.ELECTRIC_SPARK, Color.BLUE, 1.0)
        }
    }

    fun increment(): Boolean {
        if (stacks.get() >= maxStacks) return false
        stacks.incrementAndGet()
        stackHologram?.showHologram(recipient.location, stacks.get())
        return true
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun onExpire() {
        stackHologram?.remove()
    }

    override fun cancel() {
        onExpire()
    }
}
