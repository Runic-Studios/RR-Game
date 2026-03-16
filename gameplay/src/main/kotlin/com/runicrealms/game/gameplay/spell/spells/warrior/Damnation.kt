package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.Cone
import com.runicrealms.game.gameplay.spell.spellutil.particles.ParticleSphere
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/**
 * Primes the caster, then emits a persistent aura that damages, slows, and pulls enemies.
 * Soul-based scaling is intentionally deferred until SoulReaper is migrated.
 */
class Damnation(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps),
    DurationSpell,
    MagicDamageSpell,
    RadiusSpell,
    WarmupSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override var warmupSeconds = WARMUP
    override var description =
        "Prime yourself with unholy magic for ${warmupSeconds}s, then emit an aura for ${duration}s " +
            "that damages and drags nearby enemies."

    private var multiplier = PULL_MULTIPLIER
    private var damagePerSouls = DAMAGE_PER_SOUL
    private var radiusPerSouls = RADIUS_PER_SOUL

    override fun executeSpell(player: Player, type: SpellItemType) {
        addStatusEffect(player, RunicStatusEffect.SLOW_III, warmupSeconds, false)
        player.world.playSound(player.location, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 2.0f)
        player.world.playSound(player.location, Sound.ENTITY_TNT_PRIMED, 0.5f, 1.0f)
        Cone.coneEffect(player, Particle.DUST, Color.GREEN, warmupSeconds)
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { startDamnationAura(player) },
            (warmupSeconds * 20.0).toLong(),
        )
    }

    private fun startDamnationAura(player: Player) {
        val souls =
            (spellManager.getSpell(SoulReaper.SPELL_NAME) as? SoulReaper)?.getSoulCount(
                player.uniqueId
            ) ?: 0
        val totalDamage = magicDamage + (damagePerSouls * souls)
        val totalRadius = radius + (radiusPerSouls * souls)

        var count = 1.0
        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    count += 0.25
                    if (count > duration) {
                        task.cancel()
                        return@Runnable
                    }

                    if (count % 1.0 == 0.0) {
                        player.world.playSound(
                            player.location,
                            Sound.BLOCK_LAVA_EXTINGUISH,
                            0.5f,
                            1.0f,
                        )
                        ParticleSphere.show(
                            player.location,
                            Color.fromRGB(185, 251, 185),
                            totalRadius,
                            50,
                        )
                    }

                    for (entity in
                        player.world.getNearbyEntities(
                            player.location,
                            totalRadius,
                            totalRadius,
                            totalRadius,
                        ) { target ->
                            isValidEnemy(player, target)
                        }) {
                        val victim = entity as? LivingEntity ?: continue
                        if (count % 1.0 == 0.0) {
                            val dmgEvent =
                                MagicDamageEvent(totalDamage.toInt(), victim, player, this)
                            Bukkit.getPluginManager().callEvent(dmgEvent)
                            if (!dmgEvent.isCancelled) {
                                victim.damage(dmgEvent.amount.toDouble(), player)
                            }
                        }

                        val directionToMiddle =
                            player.location.toVector().subtract(victim.location.toVector())
                        if (directionToMiddle.lengthSquared() > 0) {
                            directionToMiddle.y = 0.0
                            directionToMiddle.normalize().multiply(multiplier)
                            victim.velocity = directionToMiddle
                        }
                        addStatusEffect(victim, RunicStatusEffect.SLOW_II, 1.0, false)
                    }
                },
                0L,
                5L,
            )
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        damagePerSouls = config.getDouble("magic-damage-per-souls", damagePerSouls)
        multiplier = config.getDouble("multiplier", multiplier)
        radiusPerSouls = config.getDouble("radius-per-souls", radiusPerSouls)
    }

    companion object {
        const val SPELL_NAME = "Damnation"
        const val COOLDOWN = 20.0
        const val MANA_COST = 40
        const val DURATION = 6.0
        const val BASE_DAMAGE = 20.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 4.0
        const val WARMUP = 2.0
        const val PULL_MULTIPLIER = 0.4
        const val DAMAGE_PER_SOUL = 0.0
        const val RADIUS_PER_SOUL = 0.0
    }
}
