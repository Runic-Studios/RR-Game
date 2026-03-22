package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.warrior.BleedEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.SlashEffect
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/**
 * Repeated frontal cleaves over a short duration. Final hit applies Bleed. Uses custom config key
 * `tick` for slash cadence.
 */
class Cleave(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), DurationSpell, PhysicalDamageSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var physicalDamage = BASE_DAMAGE
    override var physicalDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    private var tick = TICK_SECONDS
    override val description: String
        get() =
            "Brutally slash around yourself, dealing ($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) " +
                "physical⚔ damage every ${TICK_SECONDS}s for ${duration}s. The final slash applies &cbleed&7."

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        tick = config.getDouble("tick", tick)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        val count = AtomicInteger(0)
        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    if (count.get() >= duration.toInt()) {
                        task.cancel()
                        return@Runnable
                    }
                    cleaveEffect(player, count.get())
                    count.incrementAndGet()
                },
                0L,
                (tick * 20.0).toLong().coerceAtLeast(1L),
            )
    }

    private fun cleaveEffect(player: Player, count: Int) {
        val maxAngleCos = cos(Math.toRadians(ANGLE_DEGREES))
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.75f, 0.5f)
        SlashEffect.slashHorizontal(player, Particle.DUST, player.location)

        for (entity in
            player.world.getNearbyEntities(player.location, radius, radius, radius) { target ->
                isValidEnemy(player, target)
            }) {
            val victim = entity as? LivingEntity ?: continue
            val directionToEntity =
                victim.location.clone().subtract(player.location).toVector().normalize()
            val dot = player.location.direction.dot(directionToEntity)
            if (dot < maxAngleCos) continue

            val dmgEvent =
                PhysicalDamageEvent(physicalDamage.toInt(), victim, player, false, false, this)
            Bukkit.getPluginManager().callEvent(dmgEvent)
            if (!dmgEvent.isCancelled) {
                victim.damage(dmgEvent.amount.toDouble(), player)
            }

            if (count >= duration.toInt() - 1) {
                val bleedOpt =
                    getSpellEffect(player.uniqueId, victim.uniqueId, SpellEffectType.BLEED)
                if (bleedOpt.isEmpty) {
                    BleedEffect(
                            caster = player,
                            recipient = victim,
                            duration = BLEED_DURATION,
                            spellEffectAPI = deps.spellEffectAPI,
                            damageHandler = deps.damageHandler,
                        )
                        .initialize()
                }
            }
        }
    }

    companion object {
        const val SPELL_NAME = "Cleave"
        const val COOLDOWN = 10.0
        const val MANA_COST = 20
        const val DURATION = 4.0
        const val BASE_DAMAGE = 16.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 4.0
        const val TICK_SECONDS = 1.0
        const val BLEED_DURATION = 6.0
        const val ANGLE_DEGREES = 180.0
    }
}
