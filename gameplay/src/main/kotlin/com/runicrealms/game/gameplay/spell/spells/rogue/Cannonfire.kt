package com.runicrealms.game.gameplay.spell.spells.rogue

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
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/** Fires a spread of shrapnel pellets that damage, slow, and knock back targets. */
class Cannonfire(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell, PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override var description =
        "You fire a flurry of $TOTAL_PELLETS shrapnel fragments. On hit, each deals " +
            "($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) physical⚔ damage, slows for " +
            "${duration}s, and launches the target back."

    private var knockbackMultiplier = KNOCKBACK_MULTIPLIER
    private val hasBeenHit: MutableMap<UUID, MutableSet<UUID>> = HashMap()

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        knockbackMultiplier = -1.0 * config.getDouble("knockback-multiplier", -knockbackMultiplier)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_EGG_THROW, 0.5f, 0.5f)
        player.world.playSound(player.location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 2.0f)

        val middle =
            player.eyeLocation
                .add(0.0, 1.0, 0.0)
                .direction
                .normalize()
                .multiply(PELLET_SPEED.toDouble())
        val left = rotateVectorAroundY(middle, -10.0)
        val right = rotateVectorAroundY(middle, 10.0)

        hasBeenHit.putIfAbsent(player.uniqueId, HashSet())
        firePellets(player, middle, left, right)
    }

    private fun firePellets(player: Player, vararg vectors: Vector) {
        val item = ItemStack(MATERIAL)
        for (vector in vectors) {
            val pellet = player.world.dropItem(player.eyeLocation, item)
            pellet.pickupDelay = Int.MAX_VALUE
            pellet.velocity = vector

            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                { task ->
                    if (pellet.isOnGround || pellet.isDead) {
                        pellet.remove()
                        task.cancel()
                        return@runTaskTimer
                    }

                    val location = pellet.location
                    pellet.world.spawnParticle(
                        Particle.CRIT,
                        pellet.location,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                    for (entity in pellet.world.getNearbyEntities(location, 1.5, 1.5, 1.5)) {
                        if (!isValidEnemy(player, entity)) continue
                        if (hasBeenHit[player.uniqueId]?.contains(entity.uniqueId) == true) continue
                        explode(entity, player, pellet)
                        hasBeenHit[player.uniqueId]?.add(entity.uniqueId)
                    }
                },
                0L,
                1L,
            )
        }

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { hasBeenHit[player.uniqueId]?.clear() },
            (duration * 20).toLong(),
        )
    }

    private fun explode(victim: Entity, shooter: Player, pellet: Item) {
        hasBeenHit[shooter.uniqueId]?.add(victim.uniqueId)
        pellet.remove()

        victim.world.playSound(victim.location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 1.0f)
        victim.world.spawnParticle(Particle.EXPLOSION, victim.location, 1, 0.0, 0.0, 0.0, 0.0)

        val living = victim as? LivingEntity ?: return
        val damageEvent =
            PhysicalDamageEvent(physicalDamage.toInt(), living, shooter, false, false, this)
        Bukkit.getPluginManager().callEvent(damageEvent)
        if (!damageEvent.isCancelled) {
            living.damage(damageEvent.amount.toDouble(), shooter)
        }

        addStatusEffect(living, RunicStatusEffect.SLOW_II, duration, true)
        if (CannonfireBossUtil.isBoss(victim)) return
        val force =
            shooter.location
                .toVector()
                .subtract(victim.location.toVector())
                .normalize()
                .multiply(knockbackMultiplier)
        victim.velocity = force
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        hasBeenHit.remove(event.player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Cannonfire"
        const val COOLDOWN = 10.0
        const val MANA_COST = 30
        const val DURATION = 3.0
        const val PHYSICAL_DAMAGE = 8.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 0.5
        const val PELLET_SPEED = 2
        const val TOTAL_PELLETS = 5
        val MATERIAL: Material = Material.FIREWORK_STAR
        const val KNOCKBACK_MULTIPLIER = -1.0
    }
}

private object CannonfireBossUtil {
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
