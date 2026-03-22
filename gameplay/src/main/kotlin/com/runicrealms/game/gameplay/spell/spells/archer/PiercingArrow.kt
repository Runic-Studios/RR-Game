package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

class PiercingArrow(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), DistanceSpell, PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var distance = DISTANCE
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override val description: String
        get() =
            "Launch an arrow up to ${distance} blocks that pierces enemies, " +
                "dealing (${physicalDamage} + ${physicalDamagePerLevel}x lvl) physical damage."

    private val hitEntityMap: MutableMap<UUID, MutableSet<UUID>> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_ARROW_SHOOT, 0.5f, 0.5f)
        player.world.playSound(player.location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 2.0f)
        var start = player.eyeLocation
        val direction = player.location.direction.normalize()
        var remainingDistance = distance
        val hitSet = hitEntityMap.getOrPut(player.uniqueId) { HashSet() }

        while (remainingDistance > 0) {
            val ray =
                player.world.rayTraceEntities(start, direction, remainingDistance, BEAM_WIDTH) {
                    entity ->
                    isValidEnemy(player, entity) && !hitSet.contains(entity.uniqueId)
                }

            if (ray == null) {
                val end = start.clone().add(direction.clone().multiply(remainingDistance))
                end.direction = direction
                VectorUtil.drawLine(player, Color.fromRGB(210, 180, 140), start, end, 0.5)
                hitEntityMap.remove(player.uniqueId)
                end.world?.spawnParticle(Particle.CRIT, end, 8, 0.8, 0.5, 0.8, 0.0)
                break
            }

            val livingEntity = ray.hitEntity as? LivingEntity ?: break
            hitSet.add(livingEntity.uniqueId)
            VectorUtil.drawLine(
                player,
                Color.fromRGB(210, 180, 140),
                start,
                livingEntity.eyeLocation,
                0.5,
            )

            val dmgEvent =
                PhysicalDamageEvent(physicalDamage.toInt(), livingEntity, player, false, true, this)
            Bukkit.getPluginManager().callEvent(dmgEvent)
            if (!dmgEvent.isCancelled) {
                livingEntity.damage(dmgEvent.amount.toDouble(), player)
            }

            val travelled = ray.hitPosition.distance(start.toVector())
            start = livingEntity.eyeLocation
            remainingDistance -= maxOf(travelled, 0.1)
        }
        hitEntityMap.remove(player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Piercing Arrow"
        const val COOLDOWN = 8.0
        const val MANA_COST = 20
        const val DISTANCE = 20.0
        const val PHYSICAL_DAMAGE = 18.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val BEAM_WIDTH = 0.5
    }
}
