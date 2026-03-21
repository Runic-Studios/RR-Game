package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import kotlin.math.cos
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class Powerslide(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DistanceSpell, PhysicalDamageSpell, DurationSpell {
    override var distance = BASE_DISTANCE
    override var physicalDamage = BASE_DAMAGE
    override var physicalDamagePerLevel = DAMAGE_PER_LEVEL
    override var duration = BASE_DEBUFF_DURATION
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Slide forward $distance blocks, damaging enemies hit and silencing them."

    var cooldownReduction = BASE_COOLDOWN_REDUCTION

    override fun executeSpell(player: Player, type: SpellItemType) {
        val origin = player.location.clone()
        val baseDirection = player.location.direction
        val direction = baseDirection.clone().setY(0).normalize().multiply(distance)
        player.velocity = player.velocity.add(direction)

        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (origin.distance(player.location) >= distance) {
                    player.velocity = Vector(0, 0, 0)
                    task.cancel()
                }
            },
            0L,
            1L,
        )

        val targets = findEnemiesInCone(player, distance, Math.PI / 6.0)
        if (targets.isEmpty()) return

        val rightward = Vector(-baseDirection.z, 0.0, baseDirection.x)
        for (target in targets) {
            val targetVector =
                target.location.clone().subtract(player.location).toVector().normalize()
            val dot = targetVector.dot(rightward)
            target.velocity =
                if (dot > 0) rightward.clone().multiply(3) else rightward.clone().multiply(-3)

            val event =
                PhysicalDamageEvent(
                    physicalDamage.toInt(),
                    target,
                    player,
                    isBasicAttack = false,
                    isRanged = false,
                    spell = this,
                )
            Bukkit.getPluginManager().callEvent(event)
            if (!event.isCancelled) {
                target.damage(event.amount.toDouble(), player)
            }
            addStatusEffect(target, RunicStatusEffect.SILENCE, duration, false)
        }

        spellManager.reduceCooldown(player, SPELL_NAME, cooldownReduction)
    }

    private fun findEnemiesInCone(
        player: Player,
        maxDistance: Double,
        coneRadians: Double,
    ): List<LivingEntity> {
        val result = mutableListOf<LivingEntity>()
        val direction = player.location.direction.normalize()
        val cosThreshold = cos(coneRadians)
        for (entity in
            player.world.getNearbyEntities(
                player.location,
                maxDistance,
                maxDistance,
                maxDistance,
            )) {
            val living = entity as? LivingEntity ?: continue
            if (!isValidEnemy(player, living)) continue
            val toTarget =
                living.location.toVector().subtract(player.location.toVector()).normalize()
            if (direction.dot(toTarget) >= cosThreshold) {
                result.add(living)
            }
        }
        return result
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        cooldownReduction = loadDouble(config, "cooldown-reduction", cooldownReduction)
    }

    companion object {
        const val SPELL_NAME = "Powerslide"
        private const val BASE_DISTANCE = 8.0
        private const val BASE_DAMAGE = 16.0
        private const val DAMAGE_PER_LEVEL = 0.4
        private const val BASE_DEBUFF_DURATION = 2.0
        private const val BASE_COOLDOWN_REDUCTION = 4.0
        private const val COOLDOWN = 10.0
        private const val MANA_COST = 20
    }
}
