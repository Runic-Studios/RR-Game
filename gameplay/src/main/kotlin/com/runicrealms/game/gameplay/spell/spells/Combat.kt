package com.runicrealms.game.gameplay.spell.spells

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.CombatType
import com.runicrealms.game.gameplay.spell.event.EnterCombatEvent
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Tracks combat state per-player via a rolling cooldown. Fires a [SpellCastEvent] on entering
 * combat so the hotbar cooldown display reflects the combat timer.
 */
class Combat(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.ANY, deps) {

    override var cooldown = COMBAT_DURATION
    override var manaCost = 0
    override val description: String
        get() = "Internal: manages combat timer display."

    init {
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEnterCombat(event: EnterCombatEvent) {
        if (isOnCooldown(event.player)) return
        Bukkit.getPluginManager().callEvent(SpellCastEvent(event.player, this))
        resetCombatTimer(event.player, event.combatType)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onMagicDamage(event: MagicDamageEvent) {
        if (event.isCancelled) return
        deps.combatManager.enterCombat(
            event.caster.uniqueId,
            if (event.victim is Player) CombatType.PVP else CombatType.PVE,
        )
        val victim = event.victim
        if (victim is Player) {
            deps.combatManager.enterCombat(victim.uniqueId, CombatType.PVP)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (event.isCancelled) return
        deps.combatManager.enterCombat(
            event.caster.uniqueId,
            if (event.victim is Player) CombatType.PVP else CombatType.PVE,
        )
        val victim = event.victim
        if (victim is Player) {
            deps.combatManager.enterCombat(victim.uniqueId, CombatType.PVP)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onMobDamage(event: MobDamageEvent) {
        if (event.isCancelled) return
        val victim = event.victim
        if (victim is Player) {
            deps.combatManager.enterCombat(victim.uniqueId, CombatType.PVE)
        }
    }

    /** Resets the combat timer to full duration for the given [combatType]. */
    private fun resetCombatTimer(player: Player, combatType: CombatType) {
        spellManager.addCooldown(player, this, combatType.durationSeconds.toDouble())
    }

    companion object {
        const val SPELL_NAME = "Combat"
        const val COMBAT_DURATION = 8.0
    }
}
