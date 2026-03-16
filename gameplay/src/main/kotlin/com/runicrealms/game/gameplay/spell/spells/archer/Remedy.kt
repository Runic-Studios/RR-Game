package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class Remedy(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), DurationSpell, HealingSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var radius = RADIUS
    override var healAmount = HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var duration = DURATION
    override var description =
        "You and allies within ${radius} blocks are healed for " +
            "(${healAmount} + ${healPerLevel}x lvl) over ${duration}s."

    override fun executeSpell(player: Player, type: SpellItemType) {
        var count = 1
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count > duration) {
                    task.cancel()
                    return@runTaskTimer
                }
                count += 1
                player.world.playSound(player.location, Sound.BLOCK_GRASS_STEP, 0.5f, 0.2f)
                player.world.playSound(player.location, Sound.ENTITY_PARROT_AMBIENT, 0.5f, 0.2f)
                player.world.playSound(
                    player.location,
                    Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
                    0.25f,
                    0.2f,
                )
                createSphere(player, player.eyeLocation)
                healPlayer(player, player, healAmount, this)
                for (entity in player.getNearbyEntities(radius, radius, radius)) {
                    if (entity == player) continue
                    if (!isValidAlly(player, entity)) continue
                    val ally = entity as? Player ?: continue
                    healPlayer(player, ally, healAmount / duration, this)
                }
            },
            0L,
            20L,
        )
    }

    private fun createSphere(player: Player, location: Location) {
        repeat(PARTICLES) {
            val vector = getRandomVector().multiply(radius)
            location.add(vector)
            player.world.spawnParticle(Particle.HAPPY_VILLAGER, location, 1, 0.0, 0.0, 0.0, 0.0)
            location.subtract(vector)
        }
    }

    private fun getRandomVector(): Vector {
        val x = ThreadLocalRandom.current().nextDouble() * 2 - 1
        val y = ThreadLocalRandom.current().nextDouble() * 2 - 1
        val z = ThreadLocalRandom.current().nextDouble() * 2 - 1
        return Vector(x, y, z).normalize()
    }

    companion object {
        const val SPELL_NAME = "Remedy"
        const val COOLDOWN = 18.0
        const val MANA_COST = 35
        const val RADIUS = 7.0
        const val HEAL = 12.0
        const val HEAL_PER_LEVEL = 1.0
        const val DURATION = 5.0
        const val PARTICLES = 50
    }
}
