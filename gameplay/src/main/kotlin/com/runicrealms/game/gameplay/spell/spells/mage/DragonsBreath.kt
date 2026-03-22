package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Conjures a cone of flame in front, dealing magic damage per second for [duration]. Slows caster
 * for duration. Cancelled early by silence/stun.
 */
class DragonsBreath(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), DurationSpell, MagicDamageSpell, RadiusSpell {

    override var duration = BASE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() =
            "Breathe a cone of fire for ${duration}s, dealing $magicDamage magic damage per second."

    override fun executeSpell(player: Player, type: SpellItemType) {
        addStatusEffect(player, RunicStatusEffect.SLOW_I, duration, false)
        var ticks = 0
        val maxTicks = duration.toInt()

        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (
                    ticks >= maxTicks ||
                        hasStatusEffect(player.uniqueId, RunicStatusEffect.SILENCE) ||
                        hasStatusEffect(player.uniqueId, RunicStatusEffect.STUN)
                ) {
                    task.cancel()
                    return@runTaskTimer
                }
                fireConeHit(player)
                ticks++
            },
            0L,
            20L,
        )
    }

    private fun fireConeHit(player: Player) {
        val origin = player.location.add(0.0, 1.0, 0.0)
        val dir = origin.direction.normalize()

        for (i in 1..radius.toInt()) {
            val point = origin.clone().add(dir.clone().multiply(i))
            spawnFlameParticles(point)
            for (entity in point.world.getNearbyEntities(point, radius / 2.0, 1.0, radius / 2.0)) {
                if (entity !is LivingEntity || entity == player) continue
                if (!isValidEnemy(player, entity)) continue
                deps.damageHandler.dealMagicDamage(magicDamage.toInt(), entity, player, this)
            }
        }
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.8f)
    }

    private fun spawnFlameParticles(location: Location) {
        location.world.spawnParticle(Particle.FLAME, location, 5, 0.3, 0.2, 0.3, 0.05)
        location.world.spawnParticle(
            Particle.DUST,
            location,
            3,
            0.3,
            0.2,
            0.3,
            0.0,
            Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.0f),
        )
    }

    companion object {
        const val SPELL_NAME = "Dragon's Breath"
        const val BASE_DURATION = 3.0
        const val BASE_DAMAGE = 18.0
        const val DAMAGE_PER_LEVEL = 0.5
        const val BASE_RADIUS = 5.0
        const val COOLDOWN = 10.0
        const val MANA_COST = 30
    }
}
