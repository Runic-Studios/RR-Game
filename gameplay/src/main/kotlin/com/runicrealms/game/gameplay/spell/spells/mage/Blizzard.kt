package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
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
import org.bukkit.Bukkit
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
    override var description =
        "Mark a location. After ${warmupSeconds}s, a blizzard rains for ${duration}s."

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
                spawnSnowball(player, location)
                ticks++
            },
            0L,
            20L,
        )
    }

    private fun spawnSnowball(caster: Player, location: Location) {
        val dropFrom = location.clone().add(0.0, HEIGHT.toDouble(), 0.0)
        val snowball = caster.world.spawn(dropFrom, Snowball::class.java)
        snowball.shooter = caster
        snowball.velocity = Vector(0.0, -SNOWBALL_SPEED, 0.0)
        snowball.setMetadata(
            "blizzard_caster",
            FixedMetadataValue(deps.plugin, caster.uniqueId.toString()),
        )
    }

    @EventHandler
    fun onSnowballHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (projectile !is Snowball) return
        val casterStr =
            projectile.getMetadata("blizzard_caster").firstOrNull()?.asString() ?: return
        val caster = Bukkit.getPlayer(UUID.fromString(casterStr)) ?: return
        val hitEntity = event.hitEntity as? LivingEntity ?: return
        if (!isValidEnemy(caster, hitEntity)) return

        deps.damageHandler.dealMagicDamage(magicDamage.toInt(), hitEntity, caster, this)
        addStatusEffect(hitEntity, RunicStatusEffect.SLOW_III, SLOW_DURATION, false)
        ChilledEffect(caster, hitEntity, duration = 4.0, spellEffectAPI = deps.spellEffectAPI)
            .initialize()
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
    }
}
