package com.runicrealms.game.gameplay.spell.spells.warrior

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
import com.runicrealms.game.gameplay.spell.spellutil.ThreatUtil
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Fires a beam of light that damages a primary target and nearby enemies. Primary target is slowed;
 * secondary targets are knocked back.
 */
class Smite(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps),
    DistanceSpell,
    MagicDamageSpell,
    RadiusSpell,
    DurationSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var distance = DISTANCE
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override var duration = DURATION
    override var description =
        "Fire a beam of light that deals magic damage to the first target and nearby enemies. " +
            "The primary target is slowed for ${duration}s."

    private var knockback = KNOCKBACK

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f)
        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        if (rayTrace?.hitEntity == null) {
            val location =
                player.getTargetBlockExact(distance.toInt())?.location
                    ?: player.location.clone().add(player.location.direction.multiply(distance))
            VectorUtil.drawLine(player, Particle.CLOUD, player.eyeLocation, location, 0.5)
            player.world.spawnParticle(Particle.ANGRY_VILLAGER, location, 8, 0.5, 0.5, 0.5, 0.0)
            return
        }

        val primary = rayTrace.hitEntity as? LivingEntity ?: return
        VectorUtil.drawLine(player, Particle.CLOUD, player.eyeLocation, primary.eyeLocation, 0.75)
        primary.world.playSound(primary.location, Sound.ENTITY_GENERIC_EXPLODE, 0.25f, 2.0f)
        primary.world.spawnParticle(
            Particle.ANGRY_VILLAGER,
            primary.eyeLocation,
            8,
            0.8,
            0.5,
            0.8,
            0.0,
        )

        for (entity in
            primary.world.getNearbyEntities(primary.location, radius, radius, radius) { target ->
                isValidEnemy(player, target)
            }) {
            val victim = entity as? LivingEntity ?: continue
            if (victim.uniqueId == primary.uniqueId) continue

            val attackerPos = player.location.toVector()
            val enemyPos = victim.location.toVector()
            val knockbackDirection = enemyPos.subtract(attackerPos).normalize()
            val knockbackVector =
                knockbackDirection.multiply(knockback).setY(knockbackDirection.y + 0.15)
            victim.velocity = victim.velocity.add(knockbackVector)

            val splashEvent = MagicDamageEvent(magicDamage.toInt(), victim, player, this)
            Bukkit.getPluginManager().callEvent(splashEvent)
            if (!splashEvent.isCancelled) {
                victim.damage(splashEvent.amount.toDouble(), player)
            }
        }

        val primaryEvent = MagicDamageEvent(magicDamage.toInt(), primary, player, this)
        Bukkit.getPluginManager().callEvent(primaryEvent)
        if (!primaryEvent.isCancelled) {
            primary.damage(primaryEvent.amount.toDouble(), player)
        }
        addStatusEffect(primary, RunicStatusEffect.SLOW_II, duration, false)
        ThreatUtil.generateThreat(player, primary)
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        knockback = config.getDouble("knockback", knockback)
    }

    companion object {
        const val SPELL_NAME = "Smite"
        const val COOLDOWN = 8.0
        const val MANA_COST = 20
        const val DISTANCE = 16.0
        const val BASE_DAMAGE = 20.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 2.0
        const val DURATION = 3.0
        const val KNOCKBACK = 0.8
        const val BEAM_WIDTH = 1.5
    }
}
