package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

/**
 * Fires a bolt that brands a target. Branded targets take bonus magic damage from the caster's
 * basic attacks.
 */
class SilverBolt(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps),
    DistanceSpell,
    DurationSpell,
    MagicDamageSpell,
    PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var distance = DISTANCE
    override var duration = DURATION
    override var magicDamage = MAGIC_DAMAGE
    override var magicDamagePerLevel = MAGIC_DAMAGE_PER_LEVEL
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override val description: String
        get() =
            "You fire a silver bolt up to $distance blocks away! The first enemy hit suffers " +
                "($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) physical⚔ damage and is &7&obranded " +
                "&7for ${duration}s. &7&oBranded &7enemies take an additional " +
                "($magicDamage + &f${magicDamagePerLevel}x&7 lvl) magicʔ damage from your basic attacks!"

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ITEM_CROSSBOW_SHOOT, 0.5f, 0.5f)
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2.0f)

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
            val location = player.getTargetBlock(null, distance.toInt()).location.add(0.5, 1.0, 0.5)
            VectorUtil.drawLine(
                player,
                Color.fromRGB(200, 230, 255),
                player.eyeLocation,
                location,
                0.5,
            )
            spawnArrowTip(
                location,
                Particle.DustOptions(Color.fromRGB(210, 180, 140), 1.0f),
                player,
                1.0,
            )
            return
        }

        val victim = rayTrace.hitEntity as? LivingEntity ?: return
        VectorUtil.drawLine(
            player,
            Color.fromRGB(200, 230, 255),
            player.eyeLocation,
            victim.eyeLocation,
            0.5,
        )
        spawnArrowTip(
            victim.eyeLocation,
            Particle.DustOptions(Color.fromRGB(210, 180, 140), 1.0f),
            player,
            1.0,
        )

        val damageEvent =
            PhysicalDamageEvent(physicalDamage.toInt(), victim, player, false, true, this)
        Bukkit.getPluginManager().callEvent(damageEvent)
        if (!damageEvent.isCancelled) {
            victim.damage(damageEvent.amount.toDouble(), player)
        }

        brandedEnemies[player.uniqueId] = victim.uniqueId
        brandedTasks.remove(player.uniqueId)?.cancel()
        val task =
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable { brandedEnemies.remove(player.uniqueId) },
                (duration * 20).toLong(),
            )
        brandedTasks[player.uniqueId] = task
    }

    @EventHandler
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (event.isCancelled || !event.isBasicAttack) return
        val branded = brandedEnemies[event.caster.uniqueId] ?: return
        if (branded != event.victim.uniqueId) return

        event.caster.playSound(event.caster.location, Sound.ITEM_FIRECHARGE_USE, 0.5f, 2.0f)
        val magicEvent = MagicDamageEvent(magicDamage.toInt(), event.victim, event.caster, this)
        Bukkit.getPluginManager().callEvent(magicEvent)
        if (!magicEvent.isCancelled) {
            event.victim.damage(magicEvent.amount.toDouble(), event.caster)
        }
    }

    private fun spawnArrowTip(
        center: Location,
        dustOptions: Particle.DustOptions,
        player: Player,
        size: Double,
    ) {
        val yaw = Math.toRadians(player.location.yaw.toDouble() + 180.0)
        val cosYaw = Math.cos(yaw)
        val sinYaw = Math.sin(yaw)

        val baseDirections = arrayOf(Vector(size, 0.0, size), Vector(-size, 0.0, size))

        val directions =
            baseDirections.map { baseDirection ->
                Vector(
                    baseDirection.x * cosYaw - baseDirection.z * sinYaw,
                    baseDirection.y,
                    baseDirection.x * sinYaw + baseDirection.z * cosYaw,
                )
            }

        for (direction in directions) {
            val start = center.clone()
            val end = center.clone().add(direction)
            spawnParticleLine(start, end, dustOptions, player)
        }
    }

    private fun spawnParticleLine(
        start: Location,
        end: Location,
        dustOptions: Particle.DustOptions,
        player: Player,
    ) {
        val vector = end.toVector().subtract(start.toVector())
        val particles = (start.distance(end) * 10).toInt().coerceAtLeast(1)
        for (i in 0..particles) {
            val progress = i.toDouble() / particles.toDouble()
            val particleLocation = start.clone().add(vector.clone().multiply(progress))
            player.world.spawnParticle(Particle.DUST, particleLocation, 1, dustOptions)
        }
    }

    companion object {
        const val SPELL_NAME = "Silver Bolt"
        const val COOLDOWN = 4.0
        const val MANA_COST = 20
        const val BEAM_WIDTH = 1.5
        const val DISTANCE = 20.0
        const val DURATION = 8.0
        const val MAGIC_DAMAGE = 15.0
        const val MAGIC_DAMAGE_PER_LEVEL = 0.75
        const val PHYSICAL_DAMAGE = 24.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0

        // Internal map-based state (SPELL_MIGRATION.md): caster -> branded victim
        private val brandedEnemies: ConcurrentHashMap<UUID, UUID> = ConcurrentHashMap()
        private val brandedTasks: ConcurrentHashMap<UUID, BukkitTask> = ConcurrentHashMap()

        fun getBrandedEnemiesMap(): ConcurrentHashMap<UUID, UUID> = brandedEnemies
    }
}
