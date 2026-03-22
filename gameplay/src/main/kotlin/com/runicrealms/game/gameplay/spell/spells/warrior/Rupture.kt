package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.warrior.BleedEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellEffectEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import java.util.UUID
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Passive: after applying Bleed, your next basic attack is a critical strike. If that target is
 * bleeding, its bleed is refreshed.
 */
class Rupture(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.WARRIOR, deps) {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() =
            "After applying &cbleed&7, your next basic attack critically strikes. " +
                "If the target is bleeding, refresh their bleed."

    private val nextCriticalSet: MutableSet<UUID> = HashSet()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (!nextCriticalSet.contains(event.caster.uniqueId)) return

        event.isCritical = true
        nextCriticalSet.remove(event.caster.uniqueId)

        val bleedEffect =
            getSpellEffect(event.caster.uniqueId, event.victim.uniqueId, SpellEffectType.BLEED)
        if (bleedEffect.isPresent) {
            (bleedEffect.get() as? BleedEffect)?.initialize()
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onStatusEffect(event: SpellEffectEvent) {
        val effect = event.spellEffect
        if (effect.effectType != SpellEffectType.BLEED) return
        if (!hasPassive(effect.caster.uniqueId, name)) return
        if (nextCriticalSet.contains(effect.caster.uniqueId)) return
        nextCriticalSet.add(effect.caster.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        nextCriticalSet.remove(event.player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Rupture"
        const val COOLDOWN = 0.0
        const val MANA_COST = 0
    }
}
