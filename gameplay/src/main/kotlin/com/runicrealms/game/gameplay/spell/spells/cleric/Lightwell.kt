package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.Circle
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PotionSplashEvent

class Lightwell(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, HealingSpell, RadiusSpell {
    override var duration = BASE_DURATION
    override var healAmount = BASE_HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Sacred Spring leaves a lightwell for $duration seconds, healing allies each second."

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive - handled through PotionSplashEvent.
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPotionBreak(event: PotionSplashEvent) {
        if (!SacredSpring.thrownPotionIds.contains(event.potion.uniqueId)) return
        val player = event.potion.shooter as? Player ?: return
        if (!hasPassive(player.uniqueId, name)) return

        val location = event.potion.location.clone()
        var count = 1
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                count++
                if (count > duration.toInt()) {
                    task.cancel()
                    return@runTaskTimer
                }

                Circle.createParticleCircle(location, Particle.INSTANT_EFFECT, radius)
                player.world.playSound(location, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 0.5f)
                player.world.playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f)
                player.world.spawnParticle(Particle.FIREWORK, location, 25, 0.75, 0.75, 0.75, 0.0)

                for (entity in player.world.getNearbyEntities(location, radius, radius, radius)) {
                    if (entity is Player && isValidAlly(player, entity)) {
                        healPlayer(player, entity, healAmount, this)
                    } else if (isValidEnemy(player, entity)) {
                        entity.world.spawnParticle(
                            Particle.DUST,
                            entity.location.add(0.0, 1.0, 0.0),
                            5,
                            0.5,
                            0.5,
                            0.5,
                            0.0,
                            Particle.DustOptions(Color.BLACK, 1.0f),
                        )
                    }
                }
            },
            0L,
            20L,
        )
    }

    companion object {
        const val SPELL_NAME = "Lightwell"
        private const val BASE_DURATION = 6.0
        private const val BASE_HEAL = 10.0
        private const val HEAL_PER_LEVEL = 0.3
        private const val BASE_RADIUS = 5.0
    }
}
