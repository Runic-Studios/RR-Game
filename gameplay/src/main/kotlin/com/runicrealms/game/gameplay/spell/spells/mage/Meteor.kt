package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Fireball
import org.bukkit.entity.LargeFireball
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector

/**
 * Drops a LargeFireball from HEIGHT above target. On impact, AOE magic damage in radius. Cancels
 * the vanilla explosion.
 */
class Meteor(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), MagicDamageSpell, RadiusSpell {

    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Call down a meteor that deals $magicDamage magic damage in a $radius block radius."

    override fun executeSpell(player: Player, type: SpellItemType) {
        val target =
            player.getTargetBlockExact(MAX_DIST.toInt())?.location
                ?: player.location.add(player.location.direction.normalize().multiply(MAX_DIST))
        val spawnLoc = target.clone().add(0.0, HEIGHT.toDouble(), 0.0)

        val fireball = player.world.spawn(spawnLoc, LargeFireball::class.java)
        fireball.shooter = player
        fireball.velocity = Vector(0.0, -METEOR_SPEED, 0.0)
        fireball.setIsIncendiary(false)
        fireball.yield = 0.0f
        fireball.setMetadata(
            "meteor_caster",
            FixedMetadataValue(deps.plugin, player.uniqueId.toString()),
        )
        fireball.setMetadata(
            "meteor_data",
            FixedMetadataValue(deps.plugin, "${target.x},${target.y},${target.z}"),
        )
    }

    @EventHandler
    fun onExplosion(event: EntityExplodeEvent) {
        val fireball = event.entity as? Fireball ?: return
        if (fireball.hasMetadata("meteor_caster")) {
            event.isCancelled = true
            event.blockList().clear()
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val fireball = event.entity as? Fireball ?: return
        val casterStr = fireball.getMetadata("meteor_caster").firstOrNull()?.asString() ?: return
        val caster = Bukkit.getPlayer(UUID.fromString(casterStr)) ?: return
        val loc = fireball.location

        loc.world.spawnParticle(Particle.LAVA, loc, 30, radius, 0.5, radius)
        loc.world.spawnParticle(Particle.FLAME, loc, 50, radius, 0.5, radius, 0.1)
        loc.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f)

        for (entity in loc.world.getNearbyEntities(loc, radius, radius, radius)) {
            if (entity !is LivingEntity || entity == caster) continue
            if (!isValidEnemy(caster, entity)) continue
            deps.damageHandler.dealMagicDamage(magicDamage.toInt(), entity, caster, this)
        }
        fireball.remove()
    }

    companion object {
        const val SPELL_NAME = "Meteor"
        const val BASE_DAMAGE = 60.0
        const val DAMAGE_PER_LEVEL = 1.5
        const val BASE_RADIUS = 4.0
        const val COOLDOWN = 15.0
        const val MANA_COST = 40
        const val MAX_DIST = 12.0
        const val HEIGHT = 8
        const val METEOR_SPEED = 0.75
        const val RAY_SIZE = 1.0
    }
}
