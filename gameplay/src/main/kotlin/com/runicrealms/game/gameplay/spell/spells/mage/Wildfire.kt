package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive (Pyromancer ult passive).
 *
 * When [Fireball] deals magic damage to an enemy, also deals that same amount of damage to up to
 * [maxTargets]-1 additional enemies within [radius] blocks of the primary target. Each hit
 * (including the original) reduces Meteor's cooldown by [duration] seconds.
 */
class Wildfire(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), RadiusSpell, DurationSpell {

    override var radius = BASE_RADIUS
    override var duration = COOLDOWN_REDUCTION
    var maxTargets = MAX_TARGETS
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Passive: Fireball deals its damage to up to $MAX_TARGETS enemies within $BASE_RADIUS blocks. " +
            "Each enemy hit reduces Meteor's cooldown by ${COOLDOWN_REDUCTION}s."

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onFireballMagicDamage(event: MagicDamageEvent) {
        if (event.spell !is Fireball) return
        if (!hasPassive(event.caster.uniqueId, SPELL_NAME)) return

        // Reduce Meteor cooldown for the initial fireball hit
        spellManager.reduceCooldown(event.caster, Meteor.SPELL_NAME, duration)

        val player = event.caster
        val primaryVictim = event.victim

        var count = 1
        for (entity in
            primaryVictim.world.getNearbyEntities(primaryVictim.location, radius, radius, radius)) {
            if (entity == primaryVictim) continue
            if (entity !is LivingEntity) continue
            if (!isValidEnemy(player, entity)) continue
            count++
            if (count > maxTargets) break

            deps.damageHandler.dealMagicDamage(event.amount, entity, player, this)
            spellManager.reduceCooldown(player, Meteor.SPELL_NAME, duration)
        }
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        maxTargets = loadInt(config, "max-targets", maxTargets)
    }

    companion object {
        const val SPELL_NAME = "Wildfire"
        const val BASE_RADIUS = 5.0
        const val COOLDOWN_REDUCTION = 2.0
        const val MAX_TARGETS = 3
    }
}
