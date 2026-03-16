package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.rogue.BetrayedEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.util.Vector

/**
 * Passive. Basic attacking an enemy from behind applies the Betrayed debuff. While Betrayed is
 * active, subsequent attacks deal bonus physical damage scaled off an attribute.
 */
class Backstab(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), AttributeSpell, DurationSpell {

    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var attribute = "dexterity"
    override var attributeBaseValue = BASE_VALUE
    override var attributeMultiplier = MULTIPLIER
    override var duration = DURATION
    override var description =
        "Basic attacking an enemy from behind causes &cbetrayed &7for ${duration}s! " +
            "Subsequent attacks refresh the duration." +
            "\n\n&2&lEFFECT &cBetrayed" +
            "\n&cBetrayed &7enemies suffer ($attributeBaseValue + ${attributeMultiplier}x DEX)% " +
            "extra physical\u2694 damage from your basic attacks!"

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBackstab(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        val player = event.caster
        val uuid = player.uniqueId
        val victimId = event.victim.uniqueId

        val spellEffectOpt = getSpellEffect(uuid, victimId, SpellEffectType.BETRAYED)

        // Apply bonus damage if target is already Betrayed
        if (spellEffectOpt.isPresent) {
            // TODO: StatAPI pending (SPELL_MIGRATION.md #3)
            val bonusDamage = event.amount * attributeBaseValue
            event.amount = (event.amount + bonusDamage).toInt()
            player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.25f, 0.25f)
            event.victim.world.spawnParticle(
                Particle.ANGRY_VILLAGER,
                event.victim.eyeLocation,
                5,
                1.0,
                0.0,
                0.0,
                0.0,
            )
        }

        // Apply Betrayed debuff if attacking from behind
        if (!hasPassive(uuid, SPELL_NAME)) return
        if (!isBehind(player, event.victim)) return

        player.world.playSound(event.victim.location, Sound.ENTITY_IRON_GOLEM_DAMAGE, 0.5f, 2.0f)

        if (spellEffectOpt.isPresent) {
            (spellEffectOpt.get() as? BetrayedEffect)
                ?.initialize() // re-initialize to refresh timer
        } else {
            BetrayedEffect(
                    caster = player,
                    recipient = event.victim,
                    duration = duration,
                    spellEffectAPI = deps.spellEffectAPI,
                )
                .initialize()
        }
    }

    /** Returns true if [attacker] is behind [victim]. */
    private fun isBehind(attacker: LivingEntity, victim: LivingEntity): Boolean {
        val toAttacker: Vector =
            attacker.location.toVector().subtract(victim.location.toVector()).normalize()
        val victimDir: Vector = victim.location.direction.normalize()
        val dot = victimDir.dot(toAttacker)
        return dot < 0
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        /* passive */
    }

    companion object {
        const val SPELL_NAME = "Backstab"
        const val COOLDOWN = 0.0
        const val MANA_COST = 0
        const val BASE_VALUE = 0.05
        const val MULTIPLIER = 0.01
        const val DURATION = 5.0
    }
}
