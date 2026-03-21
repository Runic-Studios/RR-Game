package com.runicrealms.game.gameplay.spell.effect.warrior

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.damage.DamageHandler
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.StackEffect
import com.runicrealms.game.gameplay.spell.effect.StackHologram
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * StackEffect debuff. Deals [DAMAGE_PER_TICK_PERCENT]% max HP physical damage per tick (every
 * [TICK_INTERVAL] ticks), capped at [MAX_DAMAGE_PER_TICK] vs mobs. Also reduces all incoming
 * healing by [HEALING_REDUCTION].
 *
 * Uses DamageHandler to apply physical damage tick mechanics.
 */
class BleedEffect(
    override val caster: Player,
    override val recipient: LivingEntity,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
    private val damageHandler: DamageHandler,
) : StackEffect {

    override val effectType = SpellEffectType.BLEED
    override val isBuff = false
    override val stacks = AtomicInteger(DEFAULT_STACKS)
    override val tickInterval = TICK_INTERVAL
    override val stackHologram: StackHologram? =
        if (recipient is Player) {
            StackHologram(
                SpellEffectType.BLEED,
                recipient.location,
                "stacks_BLEED_${caster.uniqueId}_${recipient.uniqueId}",
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
        val maxHp = recipient.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        val damage = (maxHp * DAMAGE_PER_TICK_PERCENT).coerceAtMost(MAX_DAMAGE_PER_TICK.toDouble())
        damageHandler.dealPhysicalDamage(damage.toInt(), recipient, caster)
        recipient.world.spawnParticle(
            Particle.DUST,
            recipient.location.add(0.0, 1.0, 0.0),
            8,
            0.3,
            0.3,
            0.3,
            Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f),
        )
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

    companion object {
        const val DEFAULT_STACKS = 3
        const val HEALING_REDUCTION = 0.25
        private const val DAMAGE_PER_TICK_PERCENT = 0.03
        private const val MAX_DAMAGE_PER_TICK = 100
        private const val TICK_INTERVAL = 40
    }
}
