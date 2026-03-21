package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import java.util.UUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.util.BlockIterator
import org.bukkit.util.Vector

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
        val loc = player.location
        var validFinalBlock: org.bukkit.block.Block? = null

        val iterator: BlockIterator
        try {
            iterator = BlockIterator(player, distance.toInt())
        } catch (exception: IllegalStateException) {
            player.sendMessage(Component.text("You cannot blink here!", NamedTextColor.RED))
            return
        }

        while (iterator.hasNext()) {
            val currentBlock = iterator.next()
            val currentType = currentBlock.type

            if (
                currentType == Material.BARRIER ||
                    currentBlock.getRelative(BlockFace.UP).type == Material.BARRIER
            ) {
                break
            }

            if (currentType.isAir) {
                if (currentBlock.getRelative(BlockFace.UP).type.isAir) {
                    validFinalBlock = currentBlock
                }
            } else {
                break
            }
        }

        if (validFinalBlock == null) {
            player.sendMessage(Component.text("You cannot blink here!", NamedTextColor.RED))
            return
        }

        val teleportLoc: Location = validFinalBlock.location.clone().add(0.5, 0.0, 0.5)
        teleportLoc.pitch = loc.pitch
        teleportLoc.yaw = loc.yaw

        // particles at origin and destination
        player.world.spawnParticle(
            Particle.PORTAL,
            loc.clone().add(0.0, 1.0, 0.0),
            10,
            0.5,
            0.5,
            0.5,
        )
        player.world.spawnParticle(
            Particle.PORTAL,
            teleportLoc.clone().add(0.0, 1.0, 0.0),
            10,
            0.5,
            0.5,
            0.5,
        )
        player.world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f)

        blinkers.add(player.uniqueId)
        player.teleport(teleportLoc)

        val velocity =
            player.location.direction.add(Vector(0.0, 0.5, 0.0)).normalize().multiply(0.5)
        player.velocity = velocity

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
