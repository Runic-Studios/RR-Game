package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HelixParticleFrame
import com.runicrealms.game.gameplay.spell.spellutil.particles.SlashEffect
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler

/** Short cone slash that damages and slows, with extra silence on branded enemies. */
class Flay(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DistanceSpell, DurationSpell, PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var distance = DISTANCE
    override var duration = SLOW_DURATION
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    private var silenceDuration = SILENCE_DURATION
    override val description: String
        get() =
            "You lash out with a phantom blade, dealing ($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) " +
                "physical⚔ damage to enemies within $distance blocks and slowing them for ${duration}s. " +
                "If an affected enemy is &7&obranded&7, they are silenced for $SILENCE_DURATION s."

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        silenceDuration = config.getDouble("silence-duration", silenceDuration)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.25f)
        player.world.playSound(player.location, Sound.ENTITY_WITCH_THROW, 0.5f, 1.0f)

        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        flayEffect(player)
        val hit = rayTrace?.hitEntity as? LivingEntity ?: return
        hit.world.playSound(hit.location, Sound.ENTITY_PLAYER_HURT, 0.5f, 2.0f)

        for (entity in
            player.world.getNearbyEntities(hit.location, BEAM_WIDTH, BEAM_WIDTH, BEAM_WIDTH)) {
            val target = entity as? LivingEntity ?: continue
            if (!isValidEnemy(player, target)) continue

            HelixParticleFrame(1.0, 30.0, 40.0)
                .playParticle(player, Particle.SOUL, target.location, 0.2)
            addStatusEffect(target, RunicStatusEffect.SLOW_II, duration, false)

            val damageEvent =
                PhysicalDamageEvent(physicalDamage.toInt(), target, player, false, false, this)
            Bukkit.getPluginManager().callEvent(damageEvent)
            if (!damageEvent.isCancelled) {
                target.damage(damageEvent.amount.toDouble(), player)
            }

            if (SilverBolt.getBrandedEnemiesMap().containsValue(target.uniqueId)) {
                addStatusEffect(target, RunicStatusEffect.SILENCE, silenceDuration, true)
            }
        }
    }

    @EventHandler
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        // Listener anchor retained for parity and future cross-spell hooks.
    }

    private fun flayEffect(player: Player) {
        SlashEffect.slashVertical(player, Particle.SOUL_FIRE_FLAME, player.location, null, 1.5, 8)
    }

    companion object {
        const val SPELL_NAME = "Flay"
        const val COOLDOWN = 8.0
        const val MANA_COST = 20
        const val DISTANCE = 8.0
        const val SLOW_DURATION = 2.0
        const val PHYSICAL_DAMAGE = 24.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val SILENCE_DURATION = 2.0
        const val BEAM_WIDTH = 2.0
    }
}
