package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

class Jolt(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps),
    MagicDamageSpell,
    RadiusSpell,
    DurationSpell,
    DistanceSpell,
    WarmupSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var magicDamage = MAGIC_DAMAGE
    override var magicDamagePerLevel = MAGIC_DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override var duration = DURATION
    override var distance = DISTANCE
    override var warmupSeconds = WARMUP
    var damageInterval = DAMAGE_INTERVAL
    override var description =
        "Fire a bolt of lightning up to ${distance} blocks away! " +
            "If it hits an enemy, summon a storm in ${radius} blocks dealing " +
            "(${magicDamage} + ${magicDamagePerLevel}x lvl) magic damage every ${damageInterval}s for ${duration}s."

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        damageInterval = config.getDouble("damage-interval", damageInterval)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f)
        val ray =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                1.75,
            ) {
                isValidEnemy(player, it)
            }

        var location =
            player.getTargetBlockExact(distance.toInt())?.location
                ?: player.eyeLocation
                    .clone()
                    .add(player.location.direction.normalize().multiply(distance))
        var foundEnemy = false
        val hit = ray?.hitEntity as? LivingEntity
        if (hit != null) {
            location = hit.location
            foundEnemy = true
        }

        location.world?.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 2.0f)
        location.world?.playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.2f)
        location.world?.strikeLightningEffect(location)

        if (foundEnemy) {
            conjureLightningStorm(player, location)
        }
    }

    private fun conjureLightningStorm(player: Player, location: Location) {
        val periodTicks = maxOf(1L, (damageInterval * 20.0).toLong())
        var elapsed = 0.0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (elapsed >= duration) {
                    task.cancel()
                    return@runTaskTimer
                }
                elapsed += damageInterval
                HorizontalCircleFrame(radius / 2.0, false)
                    .playParticle(player, Particle.CRIT, location, 3.0)
                HorizontalCircleFrame(radius, false)
                    .playParticle(player, Particle.CRIT, location, 3.0)
                for (entity in
                    location.world.getNearbyEntities(location, radius, radius, radius) {
                        isValidEnemy(player, it)
                    }) {
                    val target = entity as? LivingEntity ?: continue
                    target.world.playSound(
                        target.location,
                        Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                        0.25f,
                        2.0f,
                    )
                    val event = MagicDamageEvent(magicDamage.toInt(), target, player, this)
                    Bukkit.getPluginManager().callEvent(event)
                    if (!event.isCancelled) {
                        target.damage(event.amount.toDouble(), player)
                    }
                }
            },
            0L,
            periodTicks,
        )
    }

    companion object {
        const val SPELL_NAME = "Jolt"
        const val COOLDOWN = 14.0
        const val MANA_COST = 45
        const val MAGIC_DAMAGE = 20.0
        const val MAGIC_DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 5.0
        const val DURATION = 5.0
        const val DISTANCE = 24.0
        const val WARMUP = 0.0
        const val DAMAGE_INTERVAL = 0.5
    }
}
