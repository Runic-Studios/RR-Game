package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.util.Vector

/** Dashes forward, damaging enemies passed through and reducing incoming mob damage. */
class Dash(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell, PhysicalDamageSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override var duration = DURATION
    override var radius = RADIUS
    private var launchMultiplier = LAUNCH_MULTIPLIER
    private var percent = DAMAGE_REDUCTION
    private var verticalPower = VERTICAL_POWER
    override var description =
        "You dash forward, dealing ($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) physical⚔ " +
            "damage to enemies you pass through. While dashing, you receive ${(DAMAGE_REDUCTION * 100).toInt()}% " +
            "reduced damage from monsters."

    init {
        displayCastMessage = true
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        launchMultiplier = config.getDouble("launch-multiplier", launchMultiplier)
        percent = config.getDouble("percent", percent)
        verticalPower = config.getDouble("vertical-power", verticalPower)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        val look = player.location.direction
        val launchPath =
            Vector(look.x, verticalPower, look.z).normalize().multiply(launchMultiplier)
        launchPath.y = minOf(launchPath.y, 0.95)

        addStatusEffect(player, RunicStatusEffect.SPEED_III, duration, false)
        player.world.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.0f)
        HorizontalCircleFrame(1.0, false)
            .playParticle(player, Particle.SOUL_FIRE_FLAME, player.location, 0.25, Color.FUCHSIA)
        HorizontalCircleFrame(1.0, false)
            .playParticle(player, Particle.SOUL_FIRE_FLAME, player.eyeLocation, 0.25, Color.FUCHSIA)
        player.velocity = launchPath

        lungeSet.add(player.uniqueId)
        val damagedEntities: MutableSet<Entity> = HashSet()
        val task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    val nearby =
                        player.world.getNearbyEntities(player.location, radius, radius, radius) {
                            target ->
                            isValidEnemy(player, target) && !damagedEntities.contains(target)
                        }

                    for (entity in nearby) {
                        val living = entity as? LivingEntity ?: continue
                        val damageEvent =
                            PhysicalDamageEvent(
                                physicalDamage.toInt(),
                                living,
                                player,
                                false,
                                false,
                                this,
                            )
                        Bukkit.getPluginManager().callEvent(damageEvent)
                        if (!damageEvent.isCancelled) {
                            living.damage(damageEvent.amount.toDouble(), player)
                        }
                        damagedEntities.add(entity)
                    }
                },
                0L,
                1L,
            )

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                lungeSet.remove(player.uniqueId)
                task.cancel()
            },
            (duration * 20).toLong(),
        )
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        val victim = event.victim as? Player ?: return
        if (!lungeSet.contains(victim.uniqueId)) return
        val damageReduction = (event.amount * percent).toInt()
        event.amount = (event.amount - damageReduction).coerceAtLeast(0)
    }

    companion object {
        const val SPELL_NAME = "Dash"
        const val COOLDOWN = 8.0
        const val MANA_COST = 20
        const val PHYSICAL_DAMAGE = 20.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val DURATION = 1.0
        const val RADIUS = 1.5
        const val LAUNCH_MULTIPLIER = 1.25
        const val VERTICAL_POWER = 0.25
        const val DAMAGE_REDUCTION = 0.75

        private val lungeSet: MutableSet<UUID> = HashSet()
    }
}
