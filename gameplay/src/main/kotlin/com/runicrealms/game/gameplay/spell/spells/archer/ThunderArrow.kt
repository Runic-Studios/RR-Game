package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.EntityTrail
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.util.Vector

class ThunderArrow(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), MagicDamageSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var magicDamage = MAGIC_DAMAGE
    override var magicDamagePerLevel = MAGIC_DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override val description: String
        get() =
            "Launch an enchanted arrow that deals (${magicDamage} + ${magicDamagePerLevel}x lvl) " +
                "magic damage on impact to all enemies within ${radius} blocks."

    private val powerShots: MutableSet<UUID> = HashSet()

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_ARROW_SHOOT, 0.5f, 1f)
        player.world.playSound(player.location, Sound.ITEM_FIRECHARGE_USE, 0.5f, 2f)
        val vector = player.eyeLocation.direction.normalize().multiply(2.0)
        startTask(player, vector)
    }

    @EventHandler
    fun onSearingArrowHit(event: EntityDamageByEntityEvent) {
        val arrow = event.damager as? Arrow ?: return
        if (arrow.shooter !is Player) return
        if (!powerShots.contains(arrow.uniqueId)) return
        event.isCancelled = true
    }

    private fun startTask(player: Player, vector: Vector) {
        val powerShot = player.launchProjectile(Arrow::class.java)
        powerShot.velocity = vector
        powerShot.shooter = player
        powerShots.add(powerShot.uniqueId)
        EntityTrail.entityTrail(powerShot, Particle.DUST, 5000L, Color.fromRGB(0, 71, 72))

        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                val arrowLoc: Location = powerShot.location
                if (powerShot.isDead || powerShot.isOnGround) {
                    task.cancel()
                    powerShots.remove(powerShot.uniqueId)
                    player.world.playSound(arrowLoc, Sound.BLOCK_LAVA_POP, 0.5f, 2f)
                    player.world.playSound(arrowLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 2f)
                    powerShot.world.spawnParticle(
                        Particle.EXPLOSION,
                        arrowLoc,
                        10,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                    powerShot.world.spawnParticle(Particle.CRIT, arrowLoc, 25, 0.5, 0.5, 0.5, 0.0)
                    for (entity: Entity in
                        player.world.getNearbyEntities(arrowLoc, radius, radius, radius)) {
                        val target = entity as? LivingEntity ?: continue
                        if (!isValidEnemy(player, target)) continue
                        val event = MagicDamageEvent(magicDamage.toInt(), target, player, this)
                        Bukkit.getPluginManager().callEvent(event)
                        if (!event.isCancelled) {
                            target.damage(event.amount.toDouble(), player)
                        }
                    }
                }
            },
            0L,
            1L,
        )
    }

    companion object {
        const val SPELL_NAME = "Thunder Arrow"
        const val COOLDOWN = 8.0
        const val MANA_COST = 25
        const val MAGIC_DAMAGE = 22.0
        const val MAGIC_DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 4.0
    }
}
