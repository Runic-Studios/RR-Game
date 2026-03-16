package com.runicrealms.game.gameplay.spell.effect.rogue

import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Buff/stealth on caster. On [initialize] (via SpellEffectManager), hides caster from all loaded
 * players. On [onExpire], shows caster again + plays reveal sounds/particles. Spawns SMOKE_NORMAL
 * particles once per second while active.
 */
class ShroudedEffect(
    override val caster: Player,
    override val startTime: Long = System.currentTimeMillis(),
    override val duration: Double,
    private val spellEffectAPI: SpellEffectAPI,
) : SpellEffect {

    override val recipient: LivingEntity = caster
    override val effectType = SpellEffectType.SHROUDED
    override val isBuff = true

    private var nextTick = 0

    override fun tick(globalCounter: Int) {
        if (globalCounter < nextTick) return
        executeSpellEffect()
        nextTick = globalCounter + 20
    }

    override fun executeSpellEffect() {
        caster.world.spawnParticle(
            Particle.SMOKE,
            caster.location.add(0.0, 1.0, 0.0),
            5,
            0.3,
            0.5,
            0.3,
        )
    }

    override fun initialize() {
        // Hide caster from all currently loaded players
        for (player in Bukkit.getOnlinePlayers()) {
            if (player != caster)
                player.hidePlayer(
                    spellEffectAPI.let { Bukkit.getServer().pluginManager.plugins[0] },
                    caster,
                )
        }
        spellEffectAPI.addSpellEffectToManager(this)
    }

    override fun onExpire() {
        // Reveal caster
        for (player in Bukkit.getOnlinePlayers()) {
            player.showPlayer(Bukkit.getServer().pluginManager.plugins[0], caster)
        }
        caster.world.playSound(caster.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f)
        caster.world.spawnParticle(
            Particle.LARGE_SMOKE,
            caster.location.add(0.0, 1.0, 0.0),
            20,
            0.5,
            1.0,
            0.5,
        )
    }

    override fun cancel() {
        onExpire()
    }
}
