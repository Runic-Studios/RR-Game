package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import kotlin.math.max
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent

class GrandSymphony(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps),
    RadiusSpell,
    MagicDamageSpell,
    DurationSpell,
    Tempo.Influenced {
    override var radius = BASE_RADIUS
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var duration = BASE_DURATION
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Pulse magic every second for $duration seconds, damaging and debuffing enemies."

    var debuffDuration = BASE_DEBUFF_DURATION
    var debuffRatio = BASE_DEBUFF_RATIO

    private val debuffed: MutableMap<UUID, Long> = mutableMapOf()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val activeDuration = effectiveDuration(player)
        removeExtraDuration(player)
        var pulseCount = 1
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (pulseCount.toDouble() > activeDuration || pulseCount > MAX_PULSES) {
                    task.cancel()
                    return@runTaskTimer
                }

                particleWave(player)
                val now = System.currentTimeMillis()

                for (entity in player.getNearbyEntities(radius, radius, radius)) {
                    val target = entity as? LivingEntity ?: continue
                    if (!isValidEnemy(player, target)) continue
                    val event = MagicDamageEvent(magicDamage.toInt(), target, player, this)
                    Bukkit.getPluginManager().callEvent(event)
                    if (!event.isCancelled) {
                        target.damage(event.amount.toDouble(), player)
                        debuffed[target.uniqueId] = now
                        if (pulseCount >= MAX_PULSES) {
                            addStatusEffect(target, RunicStatusEffect.STUN, debuffDuration, true)
                        }
                    }
                }

                player.world.playSound(
                    player.location,
                    Sound.BLOCK_NOTE_BLOCK_HARP,
                    SoundCategory.AMBIENT,
                    0.5f,
                    1.0f,
                )
                pulseCount++
            },
            0L,
            20L,
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBasicAttack(event: BasicAttackEvent) {
        val last = debuffed[event.player.uniqueId] ?: return
        if (System.currentTimeMillis() > last + (debuffDuration * 1000).toLong()) return
        event.cooldownTicks = event.cooldownTicks * (1.0 + debuffRatio)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        val last = debuffed[event.caster.uniqueId] ?: return
        if (System.currentTimeMillis() > last + (debuffDuration * 1000).toLong()) return
        event.amount = max(0, (event.amount * (1.0 - (debuffRatio / 2.0))).toInt())
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        val last = debuffed[event.mob.uniqueId] ?: return
        if (System.currentTimeMillis() > last + (debuffDuration * 1000).toLong()) return
        event.amount = max(0, (event.amount * (1.0 - debuffRatio)).toInt())
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        debuffed.remove(event.player.uniqueId)
    }

    private fun particleWave(player: Player) {
        val ranges = buildRanges()
        var index = 0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { particleTask ->
                if (index >= ranges.size) {
                    particleTask.cancel()
                    return@runTaskTimer
                }
                drawParticleRing(player, ranges[index])
                index++
            },
            0L,
            10L,
        )
    }

    private fun drawParticleRing(player: Player, ringRadius: Double) {
        val option = Particle.DustOptions(Color.YELLOW, 2.0f)
        for (i in 0 until PARTICLES_PER_RING) {
            val angle = (2.0 * Math.PI / PARTICLES_PER_RING) * i
            val x = player.location.x + (ringRadius * Math.cos(angle))
            val z = player.location.z + (ringRadius * Math.sin(angle))
            player.spawnParticle(
                Particle.DUST,
                x,
                player.location.y,
                z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                option,
            )
        }
    }

    private fun buildRanges(): List<Double> {
        val max = (radius * 2).toInt()
        return (1..max).map { it / 2.0 }
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        debuffDuration = loadDouble(config, "debuff-duration", debuffDuration)
        debuffRatio = loadDouble(config, "debuff-ratio", debuffRatio)
    }

    companion object {
        const val SPELL_NAME = "Grand Symphony"
        private const val COOLDOWN = 20.0
        private const val MANA_COST = 50
        private const val BASE_RADIUS = 7.0
        private const val BASE_DAMAGE = 18.0
        private const val DAMAGE_PER_LEVEL = 0.5
        private const val BASE_DURATION = 4.0
        private const val BASE_DEBUFF_DURATION = 2.0
        private const val BASE_DEBUFF_RATIO = 0.5
        private const val MAX_PULSES = 6
        private const val PARTICLES_PER_RING = 15
    }
}
