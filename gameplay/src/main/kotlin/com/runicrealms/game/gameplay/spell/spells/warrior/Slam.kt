package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

/**
 * Launches the caster into the air, then slams down to damage and knock up nearby enemies. Defines
 * [SlamLandEvent] in the companion for passive listeners (e.g. Consecrate).
 */
class Slam(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), PhysicalDamageSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var physicalDamage = BASE_DAMAGE
    override var physicalDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override var description =
        "Charge into the air, then crash down and deal " +
            "($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) physical⚔ damage in a $radius block radius."

    private val slamTasks: MutableMap<UUID, BukkitTask> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2.0f)

        val velocity = player.velocity.clone().setY(HEIGHT)
        val directionVector = player.location.direction.clone().setY(0).normalize()
        var pitch = player.eyeLocation.pitch
        if (pitch > 0.0f) pitch = -pitch
        val multiplier = (90.0f + pitch) / 50.0f
        directionVector.multiply(multiplier.toDouble())
        velocity.add(directionVector)
        velocity.multiply(Vector(0.6, 0.8, 0.6))
        player.velocity = velocity

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { slamTasks[player.uniqueId]?.cancel() },
            120L,
        )

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                player.velocity =
                    Vector(player.location.direction.x, -10.0, player.location.direction.z)
                        .multiply(2.0)
                        .normalize()
                slamTasks[player.uniqueId] = startSlamTask(player)
            },
            20L,
        )
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onFallDamage(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        if (!slamTasks.containsKey(victim.uniqueId)) return
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
            slamTasks.remove(victim.uniqueId)
        }
    }

    private fun startSlamTask(player: Player): BukkitTask {
        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    val below = player.location.clone().add(0.0, -1.0, 0.0).block
                    val isOnGround = below.isCollidable
                    if (!(isOnGround || player.fallDistance == 1.0f)) return@Runnable

                    task.cancel()
                    val slamLandEvent = SlamLandEvent(player)
                    Bukkit.getPluginManager().callEvent(slamLandEvent)
                    if (slamLandEvent.isCancelled) return@Runnable

                    player.world.playSound(
                        player.location,
                        Sound.ENTITY_ENDER_DRAGON_GROWL,
                        0.5f,
                        2.0f,
                    )
                    player.world.playSound(
                        player.location,
                        Sound.ENTITY_GENERIC_EXPLODE,
                        0.25f,
                        2.0f,
                    )
                    player.world.spawnParticle(
                        Particle.DUST,
                        player.location,
                        25,
                        0.5,
                        0.5,
                        0.5,
                        Particle.DustOptions(Color.fromRGB(210, 180, 140), 2.0f),
                    )

                    for (entity in player.getNearbyEntities(radius, radius, radius)) {
                        if (!isValidEnemy(player, entity)) continue
                        val victim = entity as? LivingEntity ?: continue
                        val damageEvent =
                            PhysicalDamageEvent(
                                physicalDamage.toInt(),
                                victim,
                                player,
                                false,
                                false,
                                this,
                            )
                        Bukkit.getPluginManager().callEvent(damageEvent)
                        if (!damageEvent.isCancelled) {
                            victim.damage(damageEvent.amount.toDouble(), player)
                        }
                        val force =
                            player.location
                                .toVector()
                                .subtract(victim.location.toVector())
                                .multiply(0.0)
                                .setY(KNOCKUP_AMOUNT)
                        victim.velocity = force.normalize()
                    }
                },
                0L,
                1L,
            )
        return task
    }

    companion object {
        const val SPELL_NAME = "Slam"
        const val COOLDOWN = 10.0
        const val MANA_COST = 20
        const val BASE_DAMAGE = 18.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 4.0
        const val KNOCKUP_AMOUNT = 0.2
        const val HEIGHT = 1.2

        class SlamLandEvent(val caster: Player) : Event(), Cancellable {
            private var cancelled = false

            override fun getHandlers(): HandlerList = handlerList

            override fun isCancelled(): Boolean = cancelled

            override fun setCancelled(cancel: Boolean) {
                cancelled = cancel
            }

            companion object {
                @JvmStatic val handlerList = HandlerList()
            }
        }
    }
}
