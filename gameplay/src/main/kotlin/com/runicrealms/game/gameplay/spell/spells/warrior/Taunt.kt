package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spellutil.ThreatUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler

/** Passive: basic attacks generate extra threat against monsters. */
class Taunt(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.WARRIOR, deps) {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description = "Basic attacks passively generate threat against monsters."

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell
    }

    @EventHandler
    fun onBasicAttack(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        generateThreat(event.caster, event.victim)
    }

    private fun generateThreat(player: Player, entity: Entity) {
        if (!isValidEnemy(player, entity)) return
        entity.world.playSound(entity.location, Sound.ENTITY_BLAZE_SHOOT, 0.05f, 0.2f)
        entity.world.spawnParticle(
            Particle.ANGRY_VILLAGER,
            entity.location.add(0.0, 1.0, 0.0),
            1,
            0.3,
            0.3,
            0.3,
            0.0,
        )
        ThreatUtil.generateThreat(player, entity, 1000.0)
    }

    companion object {
        const val SPELL_NAME = "Taunt"
        const val COOLDOWN = 0.0
        const val MANA_COST = 0
    }
}
