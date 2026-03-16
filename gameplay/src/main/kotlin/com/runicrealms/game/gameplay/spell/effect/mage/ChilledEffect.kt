package com.runicrealms.game.gameplay.spell.effect.mage

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Debuff. Spawns BLOCK_CRACK (PACKED_ICE) + BLOCK_GLASS_BREAK sound once per second on recipient.
 */
class ChilledEffect(
    override val caster: Player,
    override val recipient: LivingEntity,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val effectType = SpellEffectType.CHILLED
    override val isBuff = false

    private var nextTick = 0

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        val loc = recipient.location.add(0.0, 1.0, 0.0)
        recipient.world.spawnParticle(
            Particle.BLOCK_CRUMBLE,
            loc,
            15,
            0.3,
            0.5,
            0.3,
            Material.PACKED_ICE.createBlockData(),
        )
        recipient.world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f)
    }

    override fun initialize() {
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun cancel() {}
}
