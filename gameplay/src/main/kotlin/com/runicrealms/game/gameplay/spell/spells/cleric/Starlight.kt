package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

class Starlight(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps),
    DistanceSpell,
    DurationSpell,
    MagicDamageSpell,
    RadiusSpell {
    override var distance = BASE_DISTANCE
    override var duration = BASE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() =
            "Release a crescent wave that damages and silences enemies. Hitting enemies refreshes nearby ally shield uptime."

    private val damageMap: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val castLocation = player.eyeLocation.clone()
        starlightEffect(player, castLocation)
        var travelled = 1.0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (travelled > distance) {
                    task.cancel()
                    damageMap.remove(player.uniqueId)
                    return@runTaskTimer
                }
                travelled += PERIOD
                castLocation.add(castLocation.direction)
                starlightEffect(player, castLocation)
            },
            0L,
            (PERIOD * 20).toLong(),
        )
    }

    private fun starlightEffect(player: Player, location: Location) {
        val hitSet = damageMap.getOrPut(player.uniqueId) { mutableSetOf() }
        player.world.spawnParticle(
            Particle.DUST,
            location,
            2,
            0.5,
            0.5,
            0.5,
            0.0,
            Particle.DustOptions(Color.YELLOW, 1.0f),
        )
        HorizontalCircleFrame(BEAM_RADIUS.toDouble(), true)
            .playParticle(player, Particle.CRIT, location, 0.3, Color.BLUE)
        player.world.playSound(location, Sound.BLOCK_GLASS_BREAK, 0.25f, 2.0f)
        player.world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.25f)

        var enemyHit = false
        for (entity in
            player.world.getNearbyEntities(
                location,
                BEAM_RADIUS.toDouble(),
                BEAM_RADIUS.toDouble(),
                BEAM_RADIUS.toDouble(),
            )) {
            val target = entity as? LivingEntity ?: continue
            if (!isValidEnemy(player, target)) continue
            if (hitSet.contains(target.uniqueId)) continue

            val event = MagicDamageEvent(magicDamage.toInt(), target, player, this)
            Bukkit.getPluginManager().callEvent(event)
            if (!event.isCancelled) {
                target.damage(event.amount.toDouble(), player)
                addStatusEffect(target, RunicStatusEffect.SILENCE, duration, true)
                hitSet.add(target.uniqueId)
                enemyHit = true
            }
        }

        if (enemyHit) {
            refreshNearbyAllyShields(player)
        }
    }

    private fun refreshNearbyAllyShields(player: Player) {
        val manager = spellManager as? SpellManager ?: return
        for (entity in player.world.getNearbyEntities(player.location, radius, radius, radius)) {
            val ally = entity as? Player ?: continue
            if (!isValidAlly(player, ally)) continue
            val payload = manager.getShieldedPlayers()[ally.uniqueId] ?: continue
            payload.shield.refresh()
        }
    }

    companion object {
        const val SPELL_NAME = "Starlight"
        private const val COOLDOWN = 7.0
        private const val MANA_COST = 16
        private const val BASE_DISTANCE = 8.0
        private const val BASE_DURATION = 2.0
        private const val BASE_DAMAGE = 13.0
        private const val DAMAGE_PER_LEVEL = 0.35
        private const val BASE_RADIUS = 8.0
        private const val PERIOD = 0.5
        private const val BEAM_RADIUS = 1
    }
}
