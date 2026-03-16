package com.runicrealms.game.gameplay.spell.effect.rogue

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Debuff. Spawns VILLAGER_ANGRY particles + ITEM_FIRECHARGE_USE sound once per second on recipient.
 */
class BetrayedEffect(
    override val caster: Player,
    override val recipient: LivingEntity,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val effectType = SpellEffectType.BETRAYED
    override val isBuff = false

    private var nextTick = 0

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        val loc = recipient.location.add(0.0, 1.5, 0.0)
        recipient.world.spawnParticle(Particle.ANGRY_VILLAGER, loc, 5, 0.3, 0.3, 0.3)
        recipient.world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.0f)
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun cancel() {}
}
