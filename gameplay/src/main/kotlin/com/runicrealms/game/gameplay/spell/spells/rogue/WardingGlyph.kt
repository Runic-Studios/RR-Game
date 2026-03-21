package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.StatusEffectEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.util.Vector

/** Places an anti-magic glyph that blocks allied debuffs and silences branded enemies. */
class WardingGlyph(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = GLYPH_DURATION
    override var radius = RADIUS
    private var durationSilence = SILENCE_DURATION
    override var description =
        "Place an anti-magic glyph in a $radius block radius for ${duration}s. Allies inside are immune to debuffs. " +
            "&7&oBranded &7enemies within the glyph are silenced for $SILENCE_DURATION s each second."
    private val glyphCasters: MutableMap<UUID, Location> = ConcurrentHashMap()

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        duration = config.getDouble("duration-glyph", duration)
        durationSilence = config.getDouble("duration-silence", durationSilence)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onStatusEffect(event: StatusEffectEvent) {
        if (event.statusEffect.isBuff) return
        if (glyphCasters.isEmpty()) return

        for ((casterId, glyphLocation) in glyphCasters) {
            val caster = deps.plugin.server.getPlayer(casterId) ?: continue

            val protectedTarget =
                event.entity.uniqueId == casterId || isValidAlly(caster, event.entity)
            if (!protectedTarget) continue
            if (event.entity.location.distanceSquared(glyphLocation) > radius * radius) continue

            event.entity.world.playSound(glyphLocation, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f)
            event.isCancelled = true
        }
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        val castLocation = player.location.clone()
        glyphCasters[player.uniqueId] = castLocation

        var count = 0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                count++
                if (count > duration.toInt()) {
                    glyphCasters.remove(player.uniqueId)
                    task.cancel()
                    return@runTaskTimer
                }

                createRunicMarking(castLocation, player)
                player.world.playSound(castLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 2.0f)
                for (entity in
                    player.world.getNearbyEntities(castLocation, radius, radius, radius)) {
                    val living = entity as? LivingEntity ?: continue
                    if (!isValidEnemy(player, living)) continue
                    if (!SilverBolt.getBrandedEnemiesMap().containsValue(living.uniqueId)) continue
                    addStatusEffect(living, RunicStatusEffect.SILENCE, durationSilence, true)
                }
            },
            0L,
            20L,
        )
    }

    private fun createRunicMarking(center: Location, player: Player) {
        HorizontalCircleFrame(radius, false).playParticle(player, Particle.SOUL, center, 0.2)
        drawPentagram(center, radius)
    }

    private fun linearInterpolation(start: Vector, end: Vector, t: Double): Vector {
        return end.clone().subtract(start).multiply(t).add(start)
    }

    private fun drawPentagram(location: Location, radius: Double) {
        val world = location.world ?: return
        val center = location.toVector()
        for (index in 0 until POINTS) {
            val start = calculatePoint(center, radius, index * RADIANS_PER_POINT)
            val end = calculatePoint(center, radius, ((index + 2) % POINTS) * RADIANS_PER_POINT)
            for (particle in 0 until PARTICLES_PER_LINE) {
                val point =
                    linearInterpolation(
                        start,
                        end,
                        particle.toDouble() / (PARTICLES_PER_LINE - 1).toDouble(),
                    )
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, point.toLocation(world), 0)
            }
        }
    }

    private fun calculatePoint(center: Vector, radius: Double, angle: Double): Vector {
        return center.clone().add(Vector(radius * Math.cos(angle), 0.0, radius * Math.sin(angle)))
    }

    companion object {
        const val SPELL_NAME = "Warding Glyph"
        const val COOLDOWN = 16.0
        const val MANA_COST = 35
        const val GLYPH_DURATION = 8.0
        const val SILENCE_DURATION = 1.5
        const val RADIUS = 5.0
        const val POINTS = 5
        const val RADIANS_PER_POINT = 2 * Math.PI / POINTS
        const val PARTICLES_PER_LINE = 25
    }
}
