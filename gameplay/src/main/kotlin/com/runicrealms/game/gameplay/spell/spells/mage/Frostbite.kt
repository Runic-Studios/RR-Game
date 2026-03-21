package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.mage.ChilledEffect
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Cone attack in front: applies ChilledEffect + normal damage. If target already chilled, consumes
 * Chilled and deals empowered damage + slow.
 */
class Frostbite(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), DistanceSpell, DurationSpell, MagicDamageSpell {

    override var distance = BASE_DISTANCE
    override var duration = CHILL_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Strike enemies in a frost cone. Consumes Chilled for empowered damage."

    private var empoweredDamage = EMPOWERED_DAMAGE
    private var empoweredDamagePerLevel = EMPOWERED_DAMAGE_PER_LEVEL
    private var slowDuration = SLOW_DURATION

    override fun executeSpell(player: Player, type: SpellItemType) {
        val origin = player.location.add(0.0, 1.0, 0.0)
        val dir = origin.direction.normalize()
        val yaw = Math.toRadians(origin.yaw.toDouble())
        val hitTargets = mutableSetOf<LivingEntity>()

        for (i in 1..distance.toInt()) {
            val point = origin.clone().add(dir.clone().multiply(i))
            // Cone width grows with distance
            val coneHalfWidth = i * RADIUS / distance

            for (entity in
                point.world.getNearbyEntities(point, coneHalfWidth, 1.5, coneHalfWidth)) {
                if (entity !is LivingEntity || entity == player || entity in hitTargets) continue
                if (!isValidEnemy(player, entity)) continue

                // Check cone angle
                val toEntity = entity.location.subtract(origin).toVector().normalize()
                val angle = Math.toDegrees(Math.acos(toEntity.dot(dir).coerceIn(-1.0, 1.0)))
                if (angle > MAX_ANGLE_DEGREES) continue

                hitTargets.add(entity)
                val isChilled = hasSpellEffect(entity.uniqueId, SpellEffectType.CHILLED)

                if (isChilled) {
                    // Consume chilled and deal empowered damage
                    deps.spellEffectAPI
                        .getSpellEffects(entity.uniqueId, SpellEffectType.CHILLED)
                        .forEach { it.cancel() }
                    deps.damageHandler.dealMagicDamage(
                        empoweredDamage.toInt(),
                        entity,
                        player,
                        this,
                    )
                    addStatusEffect(entity, RunicStatusEffect.SLOW_II, slowDuration, false)
                } else {
                    deps.damageHandler.dealMagicDamage(magicDamage.toInt(), entity, player, this)
                    ChilledEffect(
                            player,
                            entity,
                            duration = duration,
                            spellEffectAPI = deps.spellEffectAPI,
                        )
                        .initialize()
                }
            }
        }

        // Particles
        for (i in 1..distance.toInt()) {
            val point = origin.clone().add(dir.clone().multiply(i))
            point.world.spawnParticle(
                Particle.BLOCK_CRUMBLE,
                point,
                3,
                0.5,
                0.2,
                0.5,
                Material.PACKED_ICE.createBlockData(),
            )
        }
        player.world.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f)
    }

    companion object {
        const val SPELL_NAME = "Frostbite"
        const val BASE_DISTANCE = 8.0
        const val CHILL_DURATION = 3.0
        const val BASE_DAMAGE = 20.0
        const val DAMAGE_PER_LEVEL = 0.5
        const val EMPOWERED_DAMAGE = 40.0
        const val EMPOWERED_DAMAGE_PER_LEVEL = 1.0
        const val SLOW_DURATION = 2.5
        const val COOLDOWN = 8.0
        const val MANA_COST = 20
        const val MAX_ANGLE_DEGREES = 60.0
        const val RADIUS = 4.0
    }
}
