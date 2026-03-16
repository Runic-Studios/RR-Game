package com.runicrealms.game.gameplay.spell.effect.cleric

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** Buff on a player recipient. Spawns NOTE particles once per second. */
class SongOfWarEffect(
    override val caster: Player,
    override val recipient: LivingEntity,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val effectType = SpellEffectType.SONG_OF_WAR
    override val isBuff = true

    private var nextTick = 0

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        recipient.world.spawnParticle(
            Particle.NOTE,
            recipient.location.add(0.0, 2.0, 0.0),
            3,
            0.5,
            0.1,
            0.5,
        )
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun cancel() {}
}
