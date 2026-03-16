package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import io.lumine.mythic.bukkit.MythicBukkit
import java.util.concurrent.CopyOnWriteArraySet
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** Stream projectile that creates a damaging, pulling whirlpool at the target location. */
class Whirlpool(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps),
    DistanceSpell,
    DurationSpell,
    MagicDamageSpell,
    PhysicalDamageSpell,
    RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var distance = DISTANCE
    override var duration = DURATION
    override var magicDamage = MAGIC_DAMAGE
    override var magicDamagePerLevel = MAGIC_DAMAGE_PER_LEVEL
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override var description =
        "You fire a stream of water, dealing ($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) " +
            "physical⚔ damage to the first enemy hit. On hit, a whirlpool forms at the target's location, " +
            "pulling enemies inward and dealing ($magicDamage + &f${magicDamagePerLevel}x&7 lvl) magicʔ damage per second " +
            "for ${duration}s."

    private var multiplier = PULL_MULTIPLIER
    private val whirlpools: MutableSet<Location> = CopyOnWriteArraySet()

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        multiplier = config.getDouble("multiplier", multiplier)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.0f)
        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        if (rayTrace == null || rayTrace.hitEntity == null) {
            val location = player.getTargetBlock(null, distance.toInt()).location
            VectorUtil.drawLine(
                player,
                Color.fromRGB(0, 102, 204),
                player.eyeLocation,
                location,
                0.5,
            )
            return
        }

        val victim = rayTrace.hitEntity as? LivingEntity ?: return
        VectorUtil.drawLine(
            player,
            Color.fromRGB(0, 102, 204),
            player.eyeLocation,
            victim.location,
            0.5,
        )
        val physicalEvent =
            PhysicalDamageEvent(physicalDamage.toInt(), victim, player, false, false, this)
        Bukkit.getPluginManager().callEvent(physicalEvent)
        if (!physicalEvent.isCancelled) {
            victim.damage(physicalEvent.amount.toDouble(), player)
        }
        summonWhirlpool(player, victim)
    }

    private fun summonWhirlpool(caster: Player, recipient: LivingEntity) {
        val castLocation = recipient.location.clone()
        whirlpoolEffect(caster, recipient, castLocation)
        whirlpools.add(castLocation)

        val maxIterations = (duration * 4).toInt()
        var iteration = 0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (iteration >= maxIterations) {
                    whirlpools.remove(castLocation)
                    task.cancel()
                    return@runTaskTimer
                }

                val shouldTickDamage = iteration % 4 == 0
                if (shouldTickDamage) {
                    whirlpoolEffect(caster, recipient, castLocation)
                }

                for (entity in
                    recipient.world.getNearbyEntities(castLocation, radius, radius, radius)) {
                    val target = entity as? LivingEntity ?: continue
                    if (!isValidEnemy(caster, target)) continue

                    if (shouldTickDamage) {
                        val magicEvent = MagicDamageEvent(magicDamage.toInt(), target, caster, this)
                        Bukkit.getPluginManager().callEvent(magicEvent)
                        if (!magicEvent.isCancelled) {
                            target.damage(magicEvent.amount.toDouble(), caster)
                        }
                    }

                    if (!WhirlpoolBossUtil.isBoss(target)) {
                        val pull = castLocation.clone().subtract(target.location).toVector()
                        if (pull.lengthSquared() > 0) {
                            pull.y = 0.0
                            target.velocity = pull.normalize().multiply(multiplier)
                        }
                    }
                    addStatusEffect(target, RunicStatusEffect.SLOW_II, 1.0, false)
                }
                iteration++
            },
            0L,
            5L,
        )
    }

    private fun whirlpoolEffect(caster: Player, recipient: LivingEntity, castLocation: Location) {
        HorizontalCircleFrame(radius, false)
            .playParticle(caster, Particle.DUST, castLocation, 0.2, Color.fromRGB(0, 64, 128))
        HorizontalCircleFrame((radius - 1.0).coerceAtLeast(1.0), false)
            .playParticle(caster, Particle.DUST, castLocation, 0.2, Color.fromRGB(0, 89, 179))
        HorizontalCircleFrame((radius - 2.0).coerceAtLeast(0.5), false)
            .playParticle(caster, Particle.DUST, castLocation, 0.2, Color.fromRGB(0, 102, 204))
        recipient.world.playSound(castLocation, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.0f)
    }

    fun isInWhirlPool(entity: LivingEntity): Boolean {
        for (whirlpool in whirlpools) {
            if (entity.world != whirlpool.world) continue
            if (entity.location.distance(whirlpool) <= radius) return true
        }
        return false
    }

    companion object {
        const val SPELL_NAME = "Whirlpool"
        const val COOLDOWN = 12.0
        const val MANA_COST = 30
        const val DISTANCE = 18.0
        const val DURATION = 5.0
        const val MAGIC_DAMAGE = 10.0
        const val MAGIC_DAMAGE_PER_LEVEL = 0.75
        const val PHYSICAL_DAMAGE = 18.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 4.0
        const val BEAM_WIDTH = 1.0
        const val PULL_MULTIPLIER = 0.35
    }
}

private object WhirlpoolBossUtil {
    fun isBoss(entity: Entity): Boolean {
        return try {
            MythicBukkit.inst()
                .mobManager
                .getActiveMob(entity.uniqueId)
                .map { activeMob ->
                    activeMob.hasFaction() && activeMob.faction.equals("boss", ignoreCase = true)
                }
                .orElse(false)
        } catch (_: Exception) {
            false
        }
    }
}
