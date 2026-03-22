package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.Circle
import kotlin.math.max
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

class CosmicPrism(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, RadiusSpell, ShieldingSpell {
    var percent = BASE_PERCENT
    var period = BASE_PERIOD

    override var duration = BASE_DURATION
    override var radius = BASE_RADIUS
    override var shieldAmount = BASE_SHIELD
    override var shieldPerLevel = SHIELD_PER_LEVEL
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() =
            "Summon a prism for $BASE_DURATION seconds. Allies in $BASE_RADIUS blocks gain shielding every $BASE_PERIOD seconds."

    override fun executeSpell(player: Player, type: SpellItemType) {
        val castLocation = player.location.clone()
        val world = castLocation.world ?: return
        world.playSound(castLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f)

        var elapsed = 0.0
        val periodTicks = max(1L, (period * 20.0).toLong())
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (elapsed >= duration) {
                    task.cancel()
                    world.playSound(castLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.5f)
                    world.spawnParticle(
                        Particle.FIREWORK,
                        castLocation,
                        35,
                        radius,
                        0.5,
                        radius,
                        0.1,
                    )
                    for (entity in world.getNearbyEntities(castLocation, radius, radius, radius)) {
                        val ally = entity as? Player ?: continue
                        if (!isValidAlly(player, ally)) continue
                        shieldPlayer(player, ally, percent * shieldAmount, this)
                    }
                    return@runTaskTimer
                }

                Circle.createParticleCircle(castLocation, Color.YELLOW, radius)
                world.playSound(castLocation, Sound.BLOCK_NOTE_BLOCK_PLING, 0.25f, 1.2f)
                for (entity in world.getNearbyEntities(castLocation, radius, radius, radius)) {
                    val ally = entity as? Player ?: continue
                    if (!isValidAlly(player, ally)) continue
                    shieldPlayer(player, ally, shieldAmount, this)
                }
                elapsed += period
            },
            0L,
            periodTicks,
        )
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        percent = config.getDouble("percent", percent)
        period = config.getDouble("period", period)
    }

    companion object {
        const val SPELL_NAME = "Cosmic Prism"
        private const val BASE_DURATION = 6.0
        private const val BASE_RADIUS = 6.0
        private const val BASE_SHIELD = 12.0
        private const val SHIELD_PER_LEVEL = 0.3
        private const val COOLDOWN = 14.0
        private const val MANA_COST = 25
        private const val BASE_PERCENT = 0.5
        private const val BASE_PERIOD = 1.0
    }
}
