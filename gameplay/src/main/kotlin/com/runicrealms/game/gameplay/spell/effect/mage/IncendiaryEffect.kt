package com.runicrealms.game.gameplay.spell.effect.mage

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.spellutil.particles.HelixParticleFrame
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** Buff on caster. Plays HelixParticleFrame (FLAME) once per second. */
class IncendiaryEffect(
    override val caster: Player,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val recipient: LivingEntity = caster
    override val effectType = SpellEffectType.INCENDIARY
    override val isBuff = true

    private var nextTick = 0
    // Old: HelixParticleFrame(radius=1.0, height=30, frequency=20.0) with default spacing 15 -> ~36 particles
    // New equivalent: height=2.0, radius=1.0, frequency=20.0, spacing=0.1 -> 36 particles
    private val helix = HelixParticleFrame(height = 2.0, radius = 1.0, frequency = 20.0)

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        helix.playParticle(caster, Particle.FLAME, caster.location, 0.1)
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun cancel() {}
}
