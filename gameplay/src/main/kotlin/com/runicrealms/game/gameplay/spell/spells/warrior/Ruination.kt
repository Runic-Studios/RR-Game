package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.Random
import java.util.UUID
import kotlin.math.cos
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.bukkit.util.noise.SimplexOctaveGenerator

/**
 * Passive aura trigger after enough souls. Since SoulReaper remains stubbed, this spell keeps base
 * mechanics and only triggers when `requiredSouls <= 0`.
 */
class Ruination(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), DurationSpell, MagicDamageSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override var description =
        "After meeting soul requirements, your next spell releases spirits in a frontal cone, " +
            "dealing magic damage and reducing healing received."

    private val cooldownPlayers: MutableSet<UUID> = HashSet()
    private val weakenedHealers: MutableSet<UUID> = HashSet()
    private var effectCooldown = EFFECT_COOLDOWN
    private var requiredSouls = REQUIRED_SOULS
    private var percent = HEAL_REDUCTION

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        effectCooldown = config.getDouble("effect-cooldown", effectCooldown)
        percent = config.getDouble("percent", percent)
        requiredSouls = config.getDouble("required-souls", requiredSouls)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onHeal(event: SpellHealEvent) {
        if (!weakenedHealers.contains(event.recipient.uniqueId)) return
        val reduction = event.amount * percent
        event.amount = (event.amount - reduction).toInt()
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (cooldownPlayers.contains(event.caster.uniqueId)) return
        val souls =
            (spellManager.getSpell(SoulReaper.SPELL_NAME) as? SoulReaper)?.getSoulCount(
                event.caster.uniqueId
            ) ?: 0
        if (souls < requiredSouls.toInt()) return

        val player = event.caster
        player.world.playSound(player.location, Sound.ENTITY_WITHER_DEATH, 0.35f, 0.5f)
        player.world.playSound(player.location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 0.25f)

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
                    conjureRuinationWave(player)
                },
                0L,
                20L,
            )
    }

    private fun conjureRuinationWave(player: Player) {
        cooldownPlayers.add(player.uniqueId)
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { cooldownPlayers.remove(player.uniqueId) },
            (effectCooldown * 20.0).toLong(),
        )
        player.world.playSound(player.location, Sound.ENTITY_PHANTOM_DEATH, 0.35f, 0.5f)

        val maxAngle = 45.0
        val middle = player.eyeLocation.direction.normalize()
        val vectors =
            arrayOf(
                rotateVectorAroundY(middle, -maxAngle),
                rotateVectorAroundY(middle, -maxAngle / 2.0),
                rotateVectorAroundY(middle, maxAngle / 2.0),
                rotateVectorAroundY(middle, maxAngle),
            )
        for (vector in vectors) {
            soulWaveEffect(player, vector, player.eyeLocation)
        }

        val maxAngleCos = cos(Math.toRadians(maxAngle))
        for (entity in player.world.getNearbyEntities(player.location, radius, radius, radius)) {
            val victim = entity as? LivingEntity ?: continue
            val directionToEntity =
                victim.location.clone().subtract(player.location).toVector().normalize()
            val dot = player.location.direction.dot(directionToEntity)
            if (dot < maxAngleCos) continue
            if (!isValidEnemy(player, victim)) continue

            weakenedHealers.add(victim.uniqueId)
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable { weakenedHealers.remove(victim.uniqueId) },
                (duration * 20.0).toLong(),
            )
            victim.world.playSound(victim.location, Sound.ENTITY_PLAYER_HURT, 0.5f, 1.0f)
            val dmgEvent = MagicDamageEvent(magicDamage.toInt(), victim, player, this)
            Bukkit.getPluginManager().callEvent(dmgEvent)
            if (!dmgEvent.isCancelled) {
                victim.damage(dmgEvent.amount.toDouble(), player)
            }
        }
    }

    private fun soulWaveEffect(player: Player, vector: Vector, location: Location) {
        val look = vector.normalize()
        val world = player.world
        val distanceStep = 0.5
        val generator = SimplexOctaveGenerator(Random(), 1).apply { setScale(0.5) }

        var distanceStepper = 0.5
        while (distanceStepper <= radius - 0.5) {
            val yOffset = generator.noise(distanceStepper, 0.0, 0.0) * 0.5
            val offset = Vector(0.0, yOffset, 0.0)
            val particleDirection = look.clone().multiply(distanceStepper).add(offset)
            val particleLocation = location.clone().add(particleDirection)
            world.spawnParticle(
                Particle.DUST,
                particleLocation,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                Particle.DustOptions(Color.TEAL, 1.0f),
            )
            world.spawnParticle(
                Particle.DUST,
                particleLocation,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                Particle.DustOptions(Color.fromRGB(185, 251, 185), 1.0f),
            )
            distanceStepper += distanceStep
        }
    }

    companion object {
        const val SPELL_NAME = "Ruination"
        const val COOLDOWN = 0.0
        const val MANA_COST = 0
        const val DURATION = 4.0
        const val BASE_DAMAGE = 12.0
        const val DAMAGE_PER_LEVEL = 0.8
        const val RADIUS = 8.0
        const val EFFECT_COOLDOWN = 10.0
        const val REQUIRED_SOULS = 0.0
        const val HEAL_REDUCTION = 0.25
    }
}
