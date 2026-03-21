package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.SlashEffect
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** Two delayed fang slashes that apply base physical damage plus execute scaling. */
class TwinFangs(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DistanceSpell, PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var distance = DISTANCE
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    private var damageCap = DAMAGE_CAP
    private var percent = EXECUTE_PERCENT
    override var description =
        "You lash out with two fangs up to $distance blocks in front of you. Each fang deals " +
            "($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) physical⚔ damage plus ${(EXECUTE_PERCENT * 100).toInt()}% execute damage."

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        damageCap = config.getDouble("damage-cap", damageCap)
        percent = config.getDouble("percent", percent)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        traceEnemies(player, 0.75f)
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { traceEnemies(player, 1.5f) },
            10L,
        )
    }

    private fun traceEnemies(player: Player, pitch: Float) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, pitch)
        player.world.playSound(player.location, Sound.ENTITY_SPIDER_DEATH, 0.5f, pitch)

        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        fangEffect(player)
        val hit = rayTrace?.hitEntity as? LivingEntity ?: return
        hit.world.playSound(hit.location, Sound.ENTITY_PLAYER_HURT, 0.5f, 2.0f)

        val targets =
            player.world.getNearbyEntities(hit.location, BEAM_WIDTH, BEAM_WIDTH, BEAM_WIDTH) {
                target ->
                isValidEnemy(player, target)
            }

        for (target in targets) {
            val living = target as? LivingEntity ?: continue
            val executeBonus = percentMissingHealth(living, percent, damageCap.toInt())
            val damageEvent =
                PhysicalDamageEvent(
                    (physicalDamage + executeBonus).toInt(),
                    living,
                    player,
                    false,
                    false,
                    this,
                )
            Bukkit.getPluginManager().callEvent(damageEvent)
            if (!damageEvent.isCancelled) {
                living.damage(damageEvent.amount.toDouble(), player)
            }
        }
    }

    private fun fangEffect(player: Player) {
        SlashEffect.slashVertical(
            player,
            Particle.DUST,
            player.location.clone(),
            Color.LIME,
            1.5,
            8,
        )
        SlashEffect.slashVertical(
            player,
            Particle.DUST,
            player.location.clone(),
            Color.LIME,
            1.5,
            8,
        )
    }

    companion object {
        const val SPELL_NAME = "Twin Fangs"
        const val COOLDOWN = 8.0
        const val MANA_COST = 25
        const val DISTANCE = 8.0
        const val PHYSICAL_DAMAGE = 18.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val DAMAGE_CAP = 500.0
        const val EXECUTE_PERCENT = 0.15
        const val BEAM_WIDTH = 2.0
    }
}
