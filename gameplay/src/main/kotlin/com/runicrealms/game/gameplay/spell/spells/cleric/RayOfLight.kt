package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

class RayOfLight(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps),
    DistanceSpell,
    DurationSpell,
    MagicDamageSpell,
    RadiusSpell {
    override var distance = BASE_MAX_DISTANCE
    override var duration = BASE_SILENCE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description = "Call down a ray of light on a target up to $distance blocks away."

    var knockback = BASE_KNOCKBACK

    override fun executeSpell(player: Player, type: SpellItemType) {
        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        val location =
            when {
                rayTrace == null ->
                    player.getTargetBlockExact(distance.toInt())?.location ?: player.location
                rayTrace.hitEntity != null -> rayTrace.hitEntity!!.location
                rayTrace.hitBlock != null -> rayTrace.hitBlock!!.location
                else -> player.getTargetBlockExact(distance.toInt())?.location ?: player.location
            }
        lightBlast(player, location)
    }

    private fun explode(player: Player, location: Location) {
        val world = player.world
        world.strikeLightningEffect(location)
        world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.0f)
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f)
        world.spawnParticle(
            Particle.EXPLOSION,
            location.clone().add(0.0, 1.0, 0.0),
            15,
            0.25,
            0.0,
            0.25,
            0.0,
        )
        for (entity in world.getNearbyEntities(location, radius, radius, radius)) {
            val target = entity as? LivingEntity ?: continue
            if (!isValidEnemy(player, target)) continue
            val force =
                player.location
                    .toVector()
                    .subtract(entity.location.toVector())
                    .multiply(-knockback)
                    .setY(0.3)
            target.velocity = force
            val event = MagicDamageEvent(magicDamage.toInt(), target, player, this)
            Bukkit.getPluginManager().callEvent(event)
            if (!event.isCancelled) {
                target.damage(event.amount.toDouble(), player)
            }
            addStatusEffect(target, RunicStatusEffect.SILENCE, duration, true)
        }
    }

    private fun lightBlast(player: Player, location: Location) {
        val trailLoc = location.clone().add(0.0, HEIGHT.toDouble(), 0.0)
        VectorUtil.drawLine(
            player,
            Particle.ANGRY_VILLAGER,
            trailLoc,
            location.clone().subtract(0.0, 20.0, 0.0),
            2.5,
        )

        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    if (trailLoc.clone().subtract(0.0, 2.0, 0.0).block.type != Material.AIR) {
                        task.cancel()
                        deps.plugin.server.scheduler.runTask(
                            deps.plugin,
                            Runnable { explode(player, trailLoc) },
                        )
                        return@Runnable
                    }

                    player.world.playSound(trailLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f)
                    player.world.playSound(trailLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f)
                    player.world.spawnParticle(
                        Particle.INSTANT_EFFECT,
                        trailLoc,
                        25,
                        0.75,
                        0.75,
                        0.75,
                        0.0,
                    )
                    trailLoc.subtract(0.0, TRAIL_SPEED.toDouble(), 0.0)
                },
                0L,
                3L,
            )

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { task.cancel() },
            MAX_DURATION * 20L,
        )
    }

    companion object {
        const val SPELL_NAME = "Ray Of Light"
        private const val COOLDOWN = 12.0
        private const val MANA_COST = 28
        private const val BASE_MAX_DISTANCE = 8.0
        private const val BASE_SILENCE_DURATION = 2.0
        private const val BASE_DAMAGE = 20.0
        private const val DAMAGE_PER_LEVEL = 0.5
        private const val BASE_RADIUS = 3.5
        private const val BASE_KNOCKBACK = 0.35
        private const val HEIGHT = 8
        private const val MAX_DURATION = 4
        private const val TRAIL_SPEED = 2
        private const val BEAM_WIDTH = 1.0
    }
}
