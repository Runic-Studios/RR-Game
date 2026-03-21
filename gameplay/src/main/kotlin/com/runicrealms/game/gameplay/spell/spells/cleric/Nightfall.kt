package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HelixParticleFrame
import java.util.Random
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.bukkit.util.noise.SimplexOctaveGenerator

class Nightfall(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, RadiusSpell, WarmupSpell {
    override var duration = BASE_DURATION
    override var radius = BASE_RADIUS
    override var warmupSeconds = BASE_WARMUP
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Slow yourself for $warmupSeconds seconds, then unleash lunar magic that launches enemies and protects allies."

    var durationInvulnerable = BASE_INVULN_DURATION
    var knockupMultiplier = BASE_KNOCKUP

    override fun executeSpell(player: Player, type: SpellItemType) {
        addStatusEffect(player, RunicStatusEffect.SLOW_III, warmupSeconds, false)
        player.world.playSound(player.location, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 2.0f)
        player.world.playSound(player.location, Sound.ENTITY_TNT_PRIMED, 0.5f, 1.0f)
        HelixParticleFrame(1.0, 1.5, 2.0)
            .playParticle(player, Particle.DUST, player.location, 6.0, Color.YELLOW)
        HelixParticleFrame(1.0, 1.5, 2.0)
            .playParticle(player, Particle.DUST, player.location, 10.0, Color.BLACK)
        Bukkit.getScheduler()
            .runTaskLater(
                deps.plugin,
                Runnable { conjureNightfall(player) },
                (warmupSeconds * 20).toLong(),
            )
    }

    private fun conjureNightfall(player: Player) {
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.75f, 0.5f)
        val maxAngle = 45.0

        val middle = player.eyeLocation.direction.normalize()
        val vectors =
            arrayOf(
                rotateVectorAroundY(middle, -maxAngle),
                rotateVectorAroundY(middle, -maxAngle / 2),
                rotateVectorAroundY(middle, maxAngle / 2),
                rotateVectorAroundY(middle, maxAngle),
            )

        for (vector in vectors) {
            spawnWaveFlameLine(player, vector, player.eyeLocation)
        }

        val maxAngleCos = Math.cos(Math.toRadians(maxAngle))
        for (entity in player.world.getNearbyEntities(player.location, radius, radius, radius)) {
            val directionToEntity =
                entity.location.clone().subtract(player.location).toVector().normalize()
            val dot = player.location.direction.dot(directionToEntity)
            if (dot < maxAngleCos) continue

            if (isValidEnemy(player, entity) && entity is LivingEntity) {
                entity.velocity = Vector(0, 1, 0).normalize().multiply(knockupMultiplier)
                entity.addPotionEffect(
                    PotionEffect(PotionEffectType.SLOW_FALLING, (duration * 20).toInt(), 2)
                )
                deps.statusEffectAPI.purge(entity.uniqueId)
            } else if (entity is LivingEntity && isValidAlly(player, entity)) {
                addStatusEffect(
                    entity,
                    RunicStatusEffect.INVULNERABILITY,
                    durationInvulnerable,
                    true,
                )
                deps.statusEffectAPI.cleanse(entity.uniqueId)
            }
        }
    }

    private fun spawnWaveFlameLine(player: Player, vector: Vector, location: Location) {
        val look = vector.normalize()
        val world: World = player.world
        val generator = SimplexOctaveGenerator(Random(), 1)
        generator.setScale(0.5)
        var distanceStep = 0.5
        while (distanceStep <= radius - 0.5) {
            val yOffset = generator.noise(distanceStep, 0.0, 0.0) * 0.5
            val offset = Vector(0.0, yOffset, 0.0)
            val particleDirection = look.clone().multiply(distanceStep).add(offset)
            val particleLocation = location.clone().add(particleDirection)
            world.spawnParticle(
                Particle.DUST,
                particleLocation,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                Particle.DustOptions(Color.BLACK, 1.0f),
            )
            world.spawnParticle(
                Particle.BLOCK,
                particleLocation,
                1,
                Material.LAPIS_BLOCK.createBlockData(),
            )
            distanceStep += 0.5
        }
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        durationInvulnerable = loadDouble(config, "duration-invulnerable", durationInvulnerable)
        knockupMultiplier = loadDouble(config, "knockup-multiplier", knockupMultiplier)
    }

    companion object {
        const val SPELL_NAME = "Nightfall"
        private const val COOLDOWN = 16.0
        private const val MANA_COST = 35
        private const val BASE_DURATION = 4.0
        private const val BASE_RADIUS = 8.0
        private const val BASE_WARMUP = 1.0
        private const val BASE_INVULN_DURATION = 2.0
        private const val BASE_KNOCKUP = 1.0
    }
}
