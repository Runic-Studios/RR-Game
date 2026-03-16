package com.runicrealms.game.gameplay.spell.spells.rogue

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import io.lumine.mythic.bukkit.MythicBukkit
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.util.Vector

/**
 * Launches a trident harpoon. On enemy hit: deals physical damage, slows, and pulls target. On ally
 * hit: teleports caster to ally. Fires [HarpoonHitEvent] on enemy hit (used by CallOfTheDeep).
 */
class Harpoon(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell, PhysicalDamageSpell {

    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var physicalDamage = BASE_DAMAGE
    override var physicalDamagePerLevel = DAMAGE_PER_LEVEL
    override var duration = DURATION
    override var description =
        "You launch a projectile harpoon of the sea! Upon hitting an enemy, " +
            "the trident deals ($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) physical\u2694 damage " +
            "and pulls its target towards you, slowing them for ${duration}s! " +
            "If an ally is hit, you are instead teleported to their location."

    private val tridentMap: MutableMap<UUID, Trident> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val trident = player.launchProjectile(Trident::class.java)
        trident.damage = 0.0
        trident.velocity = player.location.direction.normalize().multiply(TRIDENT_SPEED)
        trident.shooter = player
        tridentMap[player.uniqueId] = trident

        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.75f)
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.5f)

        // Trailing particles
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (trident.isDead || trident.isOnGround) {
                    task.cancel()
                    trident.remove()
                    return@runTaskTimer
                }
                trident.world.spawnParticle(
                    Particle.DUST,
                    trident.location,
                    10,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    Particle.DustOptions(Color.TEAL, 1.0f),
                )
            },
            0L,
            1L,
        )
    }

    @EventHandler
    fun onTridentCollide(event: ProjectileCollideEvent) {
        if (tridentMap.isEmpty()) return
        val shooter = event.entity.shooter as? Player ?: return
        if (!tridentMap.containsKey(shooter.uniqueId)) return

        val trident = tridentMap[shooter.uniqueId] ?: return
        if (event.entity != trident) return

        trident.remove()
        tridentMap.remove(shooter.uniqueId)

        val victim = event.collidedWith as? LivingEntity ?: return

        if (isValidAlly(shooter, victim)) {
            shooter.teleport(victim.eyeLocation)
            val vel =
                shooter.location.direction.add(Vector(0.0, 0.5, 0.0)).normalize().multiply(0.5)
            shooter.velocity = vel
            return
        }

        if (isValidEnemy(shooter, victim)) {
            val playerLoc = shooter.location
            val targetLoc = victim.location
            val xDir = (playerLoc.x - targetLoc.x) / 3.0
            val zDir = (playerLoc.z - targetLoc.z) / 3.0

            victim.velocity = Vector(0.0, 0.4, 0.0)

            // Fire physical damage event
            val dmgEvent =
                PhysicalDamageEvent(physicalDamage.toInt(), victim, shooter, false, true, this)
            Bukkit.getPluginManager().callEvent(dmgEvent)
            if (!dmgEvent.isCancelled) victim.damage(dmgEvent.amount.toDouble(), shooter)

            addStatusEffect(victim, RunicStatusEffect.SLOW_III, duration, false)

            // Pull victim toward caster after short delay (skip bosses)
            if (!KnockbackUtil.isBoss(victim)) {
                deps.plugin.server.scheduler.runTaskLater(
                    deps.plugin,
                    Runnable {
                        val pullVec = Vector(xDir, 0.0, zDir).normalize().multiply(2.0).setY(0.4)
                        victim.velocity = pullVec
                        victim.world.playSound(
                            victim.location,
                            Sound.ENTITY_PLAYER_HURT,
                            0.5f,
                            1.0f,
                        )
                        victim.world.spawnParticle(
                            Particle.CRIT,
                            victim.eyeLocation,
                            5,
                            0.5,
                            0.5,
                            0.5,
                            0.0,
                        )
                    },
                    4L,
                )
            }

            Bukkit.getPluginManager().callEvent(HarpoonHitEvent(shooter, victim))
        }
    }

    companion object {
        const val SPELL_NAME = "Harpoon"
        const val COOLDOWN = 12.0
        const val MANA_COST = 30
        const val BASE_DAMAGE = 30.0
        const val DAMAGE_PER_LEVEL = 2.0
        const val DURATION = 3.0
        const val TRIDENT_SPEED = 2.5

        /** Fired when Harpoon hits an enemy. Used by CallOfTheDeep. */
        class HarpoonHitEvent(val caster: Player, val victim: LivingEntity) : Event() {
            override fun getHandlers() = handlerList

            companion object {
                @JvmStatic val handlerList = HandlerList()
            }
        }
    }
}

/**
 * Extension helper to check if an entity is a MythicMobs boss without importing MythicBukkit here.
 */
private object KnockbackUtil {
    fun isBoss(entity: LivingEntity): Boolean {
        return try {
            MythicBukkit.inst()
                .mobManager
                .getActiveMob(entity.uniqueId)
                .map { it.hasFaction() && it.faction.equals("boss", ignoreCase = true) }
                .orElse(false)
        } catch (_: Exception) {
            false
        }
    }
}
