package com.runicrealms.game.gameplay.spell.effect.archer

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.StackEffect
import com.runicrealms.game.gameplay.spell.effect.StackHologram
import com.runicrealms.game.gameplay.spell.spellutil.particles.Cone
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** StackEffect buff on caster. Visual cone when at max stacks. */
class ChargedEffect(
    override val caster: Player,
    private val maxStacks: Int,
    override val duration: Double,
    override val startTime: Long = System.currentTimeMillis(),
    private val spellEffectAPI: SpellEffectAPI,
) : StackEffect {

    override val recipient: LivingEntity = caster
    override val effectType = SpellEffectType.CHARGED
    override val isBuff = true
    override val stacks = AtomicInteger(0)
    override val tickInterval = 20
    override val stackHologram: StackHologram =
        StackHologram(
            SpellEffectType.CHARGED,
            caster.location,
            "stacks_CHARGED_${caster.uniqueId}_${caster.uniqueId}",
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
        if (stacks.get() >= maxStacks) {
            Cone.coneEffect(caster, Particle.CRIT, Color.BLUE, 1.0)
            caster.world.playSound(caster.location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.5f)
        }
    }

    fun increment() {
        if (stacks.get() < maxStacks) stacks.incrementAndGet()
        stackHologram.showHologram(caster.location, stacks.get())
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
