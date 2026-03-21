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
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import java.util.UUID
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Sends a wave of frost in a forward line up to [distance] blocks. Enemies hit take magic damage
 * and are rooted for [duration]s. If the enemy is already Chilled, consumes Chilled and stuns them
 * for [stunDuration]s instead.
 */
class SnapFreeze(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), DistanceSpell, DurationSpell, MagicDamageSpell {

    override var distance = BASE_DISTANCE
    override var duration = BASE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    var stunDuration = STUN_DURATION
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Cast a frost wave up to $BASE_DISTANCE blocks. Enemies take ($BASE_DAMAGE + ${DAMAGE_PER_LEVEL}x lvl) " +
            "magic damage and are rooted for ${BASE_DURATION}s. Chilled enemies are stunned for ${STUN_DURATION}s instead."

    /** Prevents hitting the same entity twice in one cast. UUID -> set of already-hit UUIDs. */
    private val damageMap: MutableMap<UUID, MutableSet<UUID>> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val castLocation = player.eyeLocation
        freeze(player, castLocation)

        var count = 1.0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count > distance) {
                    task.cancel()
                    damageMap.remove(player.uniqueId)
                } else {
                    count += PERIOD
                    castLocation.add(castLocation.direction)
                    freeze(player, castLocation)
                }
            },
            0L,
            1L,
        )
    }

    private fun freeze(player: Player, location: Location) {
        damageMap.getOrPut(player.uniqueId) { HashSet() }
        HorizontalCircleFrame(BEAM_RADIUS.toDouble(), semiCircle = false)
            .playParticle(player, Particle.BLOCK_CRUMBLE, location, 0.3)
        player.world.playSound(location, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f)

        for (entity in
            player.world.getNearbyEntities(
                location,
                BEAM_RADIUS.toDouble(),
                BEAM_RADIUS.toDouble(),
                BEAM_RADIUS.toDouble(),
            )) {
            if (entity !is LivingEntity || entity == player) continue
            if (!isValidEnemy(player, entity)) continue
            if (damageMap[player.uniqueId]?.contains(entity.uniqueId) == true) continue

            deps.damageHandler.dealMagicDamage(magicDamage.toInt(), entity, player, this)

            val chilledOpt =
                getSpellEffect(player.uniqueId, entity.uniqueId, SpellEffectType.CHILLED)
            if (chilledOpt.isPresent) {
                (chilledOpt.get() as ChilledEffect).cancel()
                addStatusEffect(entity, RunicStatusEffect.STUN, stunDuration, displayMessage = true)
            } else {
                addStatusEffect(entity, RunicStatusEffect.ROOT, duration, displayMessage = true)
            }

            damageMap[player.uniqueId]?.add(entity.uniqueId)
        }
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        stunDuration = loadDouble(config, "stun-duration", stunDuration)
    }

    companion object {
        const val SPELL_NAME = "Snap Freeze"
        const val BASE_DISTANCE = 12.0
        const val BASE_DURATION = 2.0
        const val BASE_DAMAGE = 25.0
        const val DAMAGE_PER_LEVEL = 0.5
        const val STUN_DURATION = 1.5
        const val COOLDOWN = 10.0
        const val MANA_COST = 20
        const val PERIOD = 0.5
        const val BEAM_RADIUS = 1
    }
}
