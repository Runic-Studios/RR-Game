package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import java.util.UUID
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent

/**
 * Teleports the player forward up to [distance] blocks through valid transparent blocks. Cancels
 * fall damage after the teleport.
 */
class Blink(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.MAGE, deps), DistanceSpell {

    override var distance = BASE_DISTANCE
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description = "Blink forward up to $distance blocks."

    private val blinkers: MutableSet<UUID> = mutableSetOf()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val origin = player.location
        val dir = origin.direction.normalize()
        var teleportLoc: Location? = null

        for (i in 1..distance.toInt()) {
            val check = origin.clone().add(dir.clone().multiply(i))
            if (!check.block.type.isAir && check.block.type.isCollidable) break
            teleportLoc = check
        }

        if (teleportLoc == null) return

        blinkers.add(player.uniqueId)
        player.teleport(teleportLoc)
        origin.world.spawnParticle(Particle.PORTAL, origin, 20, 0.3, 0.5, 0.3)
        teleportLoc.world.spawnParticle(Particle.PORTAL, teleportLoc, 20, 0.3, 0.5, 0.3)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f)

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { blinkers.remove(player.uniqueId) },
            40L,
        )
    }

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return
        val player = event.entity as? Player ?: return
        if (blinkers.contains(player.uniqueId)) {
            event.isCancelled = true
            blinkers.remove(player.uniqueId)
        }
    }

    companion object {
        const val SPELL_NAME = "Blink"
        const val BASE_DISTANCE = 12.0
        const val COOLDOWN = 8.0
        const val MANA_COST = 25
    }
}
