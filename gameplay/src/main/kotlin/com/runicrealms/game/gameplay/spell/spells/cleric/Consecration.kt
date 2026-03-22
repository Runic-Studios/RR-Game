package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.Circle
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

class Consecration(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), MagicDamageSpell, DurationSpell, RadiusSpell {
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var duration = BASE_DURATION
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() =
            "Conjure holy ground for $duration seconds, slowing and damaging enemies each second."

    override fun executeSpell(player: Player, type: SpellItemType) {
        val castLocation = player.location.clone()
        var count = 1
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count > duration.toInt()) {
                    task.cancel()
                    return@runTaskTimer
                }

                Circle.createParticleCircle(castLocation, Particle.INSTANT_EFFECT, radius)
                Circle.createParticleCircle(
                    castLocation,
                    Particle.INSTANT_EFFECT,
                    (radius - 3.0).coerceAtLeast(1.0),
                )
                player.world.playSound(
                    castLocation,
                    Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
                    0.5f,
                    2.0f,
                )

                for (entity in
                    player.world.getNearbyEntities(castLocation, radius, radius, radius)) {
                    val victim = entity as? LivingEntity ?: continue
                    if (!isValidEnemy(player, victim)) continue
                    addStatusEffect(victim, RunicStatusEffect.SLOW_III, 3.0, false)
                    val event = MagicDamageEvent(magicDamage.toInt(), victim, player, this)
                    Bukkit.getPluginManager().callEvent(event)
                    if (!event.isCancelled) {
                        victim.damage(event.amount.toDouble(), player)
                    }
                }
                count++
            },
            0L,
            20L,
        )
    }

    companion object {
        const val SPELL_NAME = "Consecration"
        private const val BASE_DAMAGE = 12.0
        private const val DAMAGE_PER_LEVEL = 0.25
        private const val BASE_DURATION = 8.0
        private const val BASE_RADIUS = 7.0
        private const val COOLDOWN = 16.0
        private const val MANA_COST = 30
    }
}
