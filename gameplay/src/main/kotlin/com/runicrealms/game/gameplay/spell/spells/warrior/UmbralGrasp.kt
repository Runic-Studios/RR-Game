package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.WitherSkull
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.util.Vector

/**
 * Launches a wither skull projectile. On hit, damages target, launches caster back, then teleports
 * caster to target for a second hit and slow.
 */
class UmbralGrasp(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), DurationSpell, MagicDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override val description: String
        get() =
            "Conjure and launch a spectral skull. On enemy hit, deal magic damage, launch backwards, " +
                "teleport to the victim, then deal damage again and slow."

    private val witherSkullMap: MutableMap<UUID, WitherSkull> = HashMap()
    private var launchMultiplier = LAUNCH_MULTIPLIER
    private var speedMultiplier = SPEED_MULTIPLIER

    override fun executeSpell(player: Player, type: SpellItemType) {
        val witherSkull = player.launchProjectile(WitherSkull::class.java)
        witherSkullMap[player.uniqueId] = witherSkull
        witherSkull.velocity = player.location.direction.normalize().multiply(speedMultiplier)
        witherSkull.shooter = player
        player.world.playSound(player.location, Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.5f)
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        launchMultiplier = config.getDouble("launch-multiplier", launchMultiplier)
        speedMultiplier = config.getDouble("speed-multiplier", speedMultiplier)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (witherSkullMap.isEmpty()) return
        val player = event.entity.shooter as? Player ?: return
        if (!witherSkullMap.containsKey(player.uniqueId)) return

        val witherSkull = witherSkullMap[player.uniqueId] ?: return
        witherSkull.remove()
        witherSkullMap.remove(player.uniqueId)
        event.isCancelled = true

        val victim = event.hitEntity as? LivingEntity ?: return
        if (!isValidEnemy(player, victim)) return

        val firstHit = MagicDamageEvent(magicDamage.toInt(), victim, player, this)
        Bukkit.getPluginManager().callEvent(firstHit)
        if (!firstHit.isCancelled) {
            victim.damage(firstHit.amount.toDouble(), player)
        }
        victim.world.spawnParticle(
            Particle.ANGRY_VILLAGER,
            victim.eyeLocation,
            3,
            0.5,
            0.5,
            0.5,
            0.0,
        )

        val look = player.location.direction
        val launchPath = Vector(-look.x, 1.5, -look.z).normalize()
        player.velocity = launchPath.multiply(launchMultiplier)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f)
        player.world.spawnParticle(
            Particle.DUST,
            player.eyeLocation,
            10,
            0.5,
            0.5,
            0.5,
            Particle.DustOptions(Color.fromRGB(185, 251, 185), 1.0f),
        )

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                player.teleport(victim)
                player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f)
                player.world.playSound(
                    player.location,
                    Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,
                    0.5f,
                    0.75f,
                )
                player.world.spawnParticle(
                    Particle.DUST,
                    player.eyeLocation,
                    10,
                    0.5,
                    0.5,
                    0.5,
                    Particle.DustOptions(Color.fromRGB(185, 251, 185), 1.0f),
                )
                addStatusEffect(victim, RunicStatusEffect.SLOW_II, duration, false)
                val secondHit = MagicDamageEvent(magicDamage.toInt(), victim, player, this)
                Bukkit.getPluginManager().callEvent(secondHit)
                if (!secondHit.isCancelled) {
                    victim.damage(secondHit.amount.toDouble(), player)
                }
                // SoulReaper stacks are applied by SoulReaper's MagicDamageEvent listener when
                // this spell deals damage.
            },
            15L,
        )
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager is WitherSkull) {
            event.damage = 0.0
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.entityType == EntityType.WITHER_SKULL) {
            event.isCancelled = true
            event.blockList().clear()
        }
    }

    companion object {
        const val SPELL_NAME = "Umbral Grasp"
        const val COOLDOWN = 12.0
        const val MANA_COST = 25
        const val DURATION = 3.0
        const val BASE_DAMAGE = 18.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val LAUNCH_MULTIPLIER = 1.0
        const val SPEED_MULTIPLIER = 2.0
    }
}
