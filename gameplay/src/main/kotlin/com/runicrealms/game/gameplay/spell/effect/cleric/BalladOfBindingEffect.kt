package com.runicrealms.game.gameplay.spell.effect.cleric

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** Debuff on a recipient from a caster. Spawns VILLAGER_ANGRY particles once per second. */
class BalladOfBindingEffect(
    override val caster: Player,
    override val recipient: LivingEntity,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val effectType = SpellEffectType.BALLAD_OF_BINDING
    override val isBuff = false

    private var nextTick = 0

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        recipient.world.spawnParticle(
            Particle.ANGRY_VILLAGER,
            recipient.location.add(0.0, 1.5, 0.0),
            5,
            0.3,
            0.3,
            0.3,
        )
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun cancel() {}
}
