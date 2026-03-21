package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.scheduler.BukkitTask

/**
 * Passive tied to Slam. Landing with Slam creates a consecrated area that damages enemies, slows
 * them, and heals allies each second.
 */
class Consecrate(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps),
    DurationSpell,
    HealingSpell,
    MagicDamageSpell,
    RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var healAmount = BASE_HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    private var slowDuration = SLOW_DURATION
    override var description =
        "Your Slam leaves consecrated ground in a $radius block radius for ${duration}s. " +
            "Enemies take ($magicDamage + &f${magicDamagePerLevel}x&7 lvl) magicʔ damage and are slowed " +
            "for ${SLOW_DURATION}s each tick. Allies are healed for ($healAmount + &f${healPerLevel}x&7 lvl)."

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        slowDuration = config.getDouble("slow-duration", slowDuration)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onSlamLand(event: Slam.Companion.SlamLandEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        consecrate(event.caster, event.caster.location.clone())
    }

    private fun consecrate(caster: Player, castLocation: Location) {
        var count = 0
        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    if (count >= duration.toInt()) {
                        task.cancel()
                        return@Runnable
                    }
                    count++

                    HorizontalCircleFrame(radius, false)
                        .playParticle(caster, Particle.DUST, castLocation, 3.0, Color.YELLOW)
                    createStarParticles(castLocation, radius, Particle.ANGRY_VILLAGER, 5)

                    for (entity in
                        castLocation.world.getNearbyEntities(
                            castLocation,
                            radius,
                            radius,
                            radius,
                        )) {
                        when {
                            isValidEnemy(caster, entity) -> {
                                val victim = entity as? LivingEntity ?: continue
                                val dmgEvent =
                                    MagicDamageEvent(magicDamage.toInt(), victim, caster, this)
                                Bukkit.getPluginManager().callEvent(dmgEvent)
                                if (!dmgEvent.isCancelled) {
                                    victim.damage(dmgEvent.amount.toDouble(), caster)
                                }
                                addStatusEffect(
                                    victim,
                                    RunicStatusEffect.SLOW_II,
                                    slowDuration,
                                    false,
                                )
                            }
                            isValidAlly(caster, entity) -> {
                                val ally = entity as? Player ?: continue
                                healPlayer(caster, ally, healAmount, this)
                            }
                        }
                    }
                },
                0L,
                20L,
            )
    }

    companion object {
        const val SPELL_NAME = "Consecrate"
        const val COOLDOWN = 0.0
        const val MANA_COST = 0
        const val DURATION = 6.0
        const val BASE_DAMAGE = 12.0
        const val DAMAGE_PER_LEVEL = 0.8
        const val BASE_HEAL = 10.0
        const val HEAL_PER_LEVEL = 0.5
        const val RADIUS = 4.0
        const val SLOW_DURATION = 2.0

        fun createStarParticles(center: Location, radius: Double, particle: Particle, points: Int) {
            val angleBetweenPoints = Math.PI * 2.0 / points
            val angleOffset = -Math.PI / 2.0
            val innerToOuterRatio = 0.5

            for (index in 0 until points * 2) {
                val currentRadius = if (index % 2 == 0) radius else radius * innerToOuterRatio
                val angle = index * angleBetweenPoints / 2.0 + angleOffset
                val x = center.x + currentRadius * kotlin.math.cos(angle)
                val z = center.z + currentRadius * kotlin.math.sin(angle)
                val point = Location(center.world, x, center.y, z)

                when (particle) {
                    Particle.FALLING_DUST -> {
                        center.world.spawnParticle(
                            particle,
                            point,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            Bukkit.createBlockData(Material.PACKED_ICE),
                        )
                    }
                    Particle.DUST -> {
                        center.world.spawnParticle(
                            particle,
                            point,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            Particle.DustOptions(Color.AQUA, 1.0f),
                        )
                    }
                    else -> {
                        center.world.spawnParticle(particle, point, 1, 0.0, 0.0, 0.0, 0.0)
                    }
                }
            }
        }
    }
}
