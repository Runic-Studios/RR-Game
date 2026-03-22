package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.mage.ChilledEffect
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import java.util.UUID
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector

/**
 * Marks a target area; after [warmupSeconds] delay, rains snowballs every second for [duration],
 * each dealing magic damage + SLOW_III + ChilledEffect.
 */
class Blizzard(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps),
    DistanceSpell,
    DurationSpell,
    MagicDamageSpell,
    RadiusSpell,
    WarmupSpell {

    override var distance = BASE_DISTANCE
    override var duration = BASE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var warmupSeconds = BASE_WARMUP
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() = "Mark a location. After ${warmupSeconds}s, a blizzard rains for ${duration}s."

    private val activeBlizzards: MutableMap<UUID, Location> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val target = player.getTargetBlockExact(distance.toInt())?.location ?: player.location
        player.world.playSound(target, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f)
        player.world.spawnParticle(
            Particle.ITEM_SNOWBALL,
            target.add(0.0, 0.5, 0.0),
            20,
            radius,
            0.5,
            radius,
        )

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { startBlizzard(player, target) },
            (warmupSeconds * 20).toLong(),
        )
    }

    private fun startBlizzard(player: Player, location: Location) {
        var ticks = 0
        val maxTicks = duration.toInt()
        activeBlizzards[player.uniqueId] = location

        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (ticks >= maxTicks) {
                    task.cancel()
                    activeBlizzards.remove(player.uniqueId)
                    return@runTaskTimer
                }
                spawnSnowballs(player, location)
                blizzardDamage(player, location)
                ticks++
            },
            0L,
            20L,
        )
    }

    private fun blizzardDamage(caster: Player, location: Location) {
        for (entity in caster.world.getNearbyEntities(location, radius, radius, radius)) {
            if (entity !is LivingEntity) continue
            if (!isValidEnemy(caster, entity)) continue
            caster.world.playSound(entity.location, Sound.BLOCK_GLASS_BREAK, 0.25f, 1.0f)
            deps.damageHandler.dealMagicDamage(magicDamage.toInt(), entity, caster, this)
            addStatusEffect(entity, RunicStatusEffect.SLOW_III, SLOW_DURATION, false)
            val existingChill =
                getSpellEffect(caster.uniqueId, entity.uniqueId, SpellEffectType.CHILLED)
            if (existingChill.isPresent) {
                existingChill.get().cancel()
            }
            ChilledEffect(caster, entity, duration = 4.0, spellEffectAPI = deps.spellEffectAPI)
                .initialize()
        }
    }

    private fun spawnSnowballs(caster: Player, location: Location) {
        val dropFrom = location.clone().add(0.0, HEIGHT.toDouble(), 0.0)
        val fixedRadius = radius - 1
        repeat(SNOWBALL_COUNT) {
            val offsetX = (Math.random() * fixedRadius * 2) - fixedRadius
            val offsetZ = (Math.random() * fixedRadius * 2) - fixedRadius
            val spawnLoc = dropFrom.clone().add(offsetX, 0.0, offsetZ)
            val snowball = caster.world.spawn(spawnLoc, Snowball::class.java)
            snowball.shooter = caster
            snowball.velocity = Vector(0.0, -SNOWBALL_SPEED, 0.0)
            snowball.setMetadata(
                "blizzard_caster",
                FixedMetadataValue(deps.plugin, caster.uniqueId.toString()),
            )
        }
    }

    @EventHandler
    fun onSnowballHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (projectile !is Snowball) return
        if (!projectile.hasMetadata("blizzard_caster")) return
        projectile.remove()
    }

    @EventHandler
    fun onSnowballDamage(event: EntityDamageByEntityEvent) {
        val projectile = event.damager as? Snowball ?: return
        if (projectile.hasMetadata("blizzard_caster")) event.isCancelled = true
    }

    companion object {
        const val SPELL_NAME = "Blizzard"
        const val BASE_DISTANCE = 15.0
        const val BASE_DURATION = 4.0
        const val BASE_DAMAGE = 15.0
        const val DAMAGE_PER_LEVEL = 0.5
        const val BASE_RADIUS = 3.0
        const val BASE_WARMUP = 1.5
        const val COOLDOWN = 12.0
        const val MANA_COST = 35
        const val HEIGHT = 9
        const val SLOW_DURATION = 2.0
        const val SNOWBALL_SPEED = 0.5
        const val RAY_SIZE = 1.0
        const val SNOWBALL_COUNT = 4
    }
}
