package com.runicrealms.game.gameplay.spell.effect.archer

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** Debuff. Spawns CRIT particles + electric sound on recipient once per second. */
class StaticEffect(
    override val caster: Player,
    override val recipient: LivingEntity,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val effectType = SpellEffectType.STATIC
    override val isBuff = false

    private var nextTick = 0

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        val loc = recipient.location.add(0.0, 1.0, 0.0)
        recipient.world.spawnParticle(Particle.CRIT, loc, 8, 0.3, 0.5, 0.3)
        recipient.world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.5f)
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun cancel() {}
}
