package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent

class RainOfArrows(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), RadiusSpell, DurationSpell, PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var radius = RADIUS
    override var duration = DURATION
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override var description =
        "Rain arrows over a ${radius}-block area for ${duration}s. " +
            "Arrows strike every ${INTERVAL}s for (${physicalDamage} + ${physicalDamagePerLevel}x lvl) physical damage. " +
            "Moving ends the channel early."

    private val casting: MutableMap<UUID, Location> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val initial = player.location.clone()
        casting[player.uniqueId] = initial
        var remaining = duration

        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                val location = casting[player.uniqueId]
                if (
                    remaining <= 0.0 ||
                        location == null ||
                        player.location.x != location.x ||
                        player.location.y != location.y ||
                        player.location.z != location.z
                ) {
                    casting.remove(player.uniqueId)
                    player.playSound(
                        player.location,
                        Sound.BLOCK_BEACON_DEACTIVATE,
                        SoundCategory.AMBIENT,
                        1f,
                        1f,
                    )
                    task.cancel()
                    return@runTaskTimer
                }

                for (index in 0 until 6) {
                    val offsetX = (ThreadLocalRandom.current().nextDouble() * 2 * radius) - radius
                    val offsetZ = (ThreadLocalRandom.current().nextDouble() * 2 * radius) - radius
                    val randomLocation = location.clone().add(offsetX, 0.0, offsetZ)
                    location.world?.playSound(randomLocation, Sound.ENTITY_ARROW_HIT, 3.0f, 1.0f)
                    VectorUtil.drawLine(
                        player,
                        Particle.CRIT,
                        randomLocation.clone().add(0.0, HEIGHT.toDouble(), 0.0),
                        randomLocation.clone().subtract(0.0, 20.0, 0.0),
                        2.0,
                    )
                }

                for (entity in player.world.getNearbyEntities(location, radius, radius, radius)) {
                    val target = entity as? LivingEntity ?: continue
                    if (!isValidEnemy(player, target)) continue
                    val dmgEvent =
                        PhysicalDamageEvent(
                            physicalDamage.toInt(),
                            target,
                            player,
                            false,
                            true,
                            this,
                        )
                    Bukkit.getPluginManager().callEvent(dmgEvent)
                    if (!dmgEvent.isCancelled) {
                        target.damage(dmgEvent.amount.toDouble(), player)
                    }
                }

                remaining -= INTERVAL
            },
            0L,
            (INTERVAL * 20.0).toLong(),
        )
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        casting.remove(event.player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Rain Of Arrows"
        const val COOLDOWN = 30.0
        const val MANA_COST = 70
        const val RADIUS = 6.0
        const val DURATION = 6.0
        const val PHYSICAL_DAMAGE = 8.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val HEIGHT = 10
        const val INTERVAL = 0.5
    }
}
