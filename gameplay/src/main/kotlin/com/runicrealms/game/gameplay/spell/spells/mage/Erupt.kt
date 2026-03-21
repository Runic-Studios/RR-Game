package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.mage.IgniteEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.util.Vector

/**
 * Erupts a fire blast at the target location, dealing AOE magic damage + knockup + IgniteEffect.
 * Listens to MagicDamageEvent from Fireball: if target is ignited, consumes the ignite and adds
 * [maxHealthPercent]% max health damage (capped at [MOB_DAMAGE_CAP] vs mobs).
 */
class Erupt(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), DurationSpell, MagicDamageSpell, RadiusSpell {

    override var duration = IGNITE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Erupt fire at a location, dealing $magicDamage magic damage and igniting enemies."

    private var maxHealthPercent = MAX_HEALTH_PERCENT
    private var knockupMultiplier = KNOCKUP_MULTIPLIER

    override fun executeSpell(player: Player, type: SpellItemType) {
        val target =
            player.getTargetBlockExact(MAX_DIST.toInt())?.location
                ?: player.location.add(player.location.direction.normalize().multiply(MAX_DIST))
        val center = target.clone().add(0.0, 0.5, 0.0)

        center.world.spawnParticle(Particle.FLAME, center, 40, radius, 0.5, radius, 0.1)
        center.world.spawnParticle(Particle.LAVA, center, 10, radius, 0.5, radius)
        center.world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f)

        for (entity in center.world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity !is LivingEntity || entity == player) continue
            if (!isValidEnemy(player, entity)) continue

            deps.damageHandler.dealMagicDamage(magicDamage.toInt(), entity, player, this)

            // Knockup
            val knockup = Vector(0.0, knockupMultiplier, 0.0)
            entity.velocity = knockup

            // Apply ignite
            IgniteEffect(
                    player,
                    entity,
                    duration = IGNITE_DURATION,
                    spellEffectAPI = deps.spellEffectAPI,
                )
                .initialize()
        }
    }

    @EventHandler
    fun onFireballDamage(event: MagicDamageEvent) {
        if (event.spell !is Fireball) return
        val victim = event.victim as? LivingEntity ?: return
        if (!hasSpellEffect(victim.uniqueId, SpellEffectType.IGNITED)) return

        // Consume ignite and deal bonus damage
        deps.spellEffectAPI.getSpellEffects(victim.uniqueId, SpellEffectType.IGNITED).forEach {
            it.cancel()
        }

        val bonusDamage =
            minOf(
                percentMaxHealth(victim, maxHealthPercent / 100.0).toDouble(),
                MOB_DAMAGE_CAP.toDouble(),
            )
        deps.damageHandler.dealMagicDamage(bonusDamage.toInt(), victim, event.caster, this)
    }

    companion object {
        const val SPELL_NAME = "Erupt"
        const val BASE_DAMAGE = 25.0
        const val DAMAGE_PER_LEVEL = 0.75
        const val BASE_RADIUS = 4.0
        const val IGNITE_DURATION = 3.0
        const val COOLDOWN = 8.0
        const val MANA_COST = 25
        const val MAX_DIST = 10.0
        const val MAX_HEALTH_PERCENT = 15.0
        const val MOB_DAMAGE_CAP = 500
        const val KNOCKUP_MULTIPLIER = 0.6
        const val RAY_SIZE = 1.5
    }
}
