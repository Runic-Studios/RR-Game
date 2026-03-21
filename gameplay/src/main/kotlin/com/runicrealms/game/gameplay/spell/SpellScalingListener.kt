package com.runicrealms.game.gameplay.spell

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.event.SpellShieldEvent
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Applies per-level spell scaling as the very first modification to damage/heal/shield values,
 * before any other calculations (crit multipliers, reductions, etc.).
 *
 * Each scaling component interface ([MagicDamageSpell], etc.) exposes a [perLevel] field that is
 * multiplied by the caster's level and added to the base amount.
 */
@Singleton
class SpellScalingListener
@Inject
constructor(private val plugin: Plugin, private val userDataRegistry: UserDataRegistry) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onMagicDamage(event: MagicDamageEvent) {
        val spell = event.spell as? MagicDamageSpell ?: return
        val level = getLevel(event.caster) ?: return
        event.amount += (spell.magicDamagePerLevel * level).toInt()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        val spell = event.spell as? PhysicalDamageSpell ?: return
        val level = getLevel(event.caster) ?: return
        event.amount += (spell.physicalDamagePerLevel * level).toInt()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onSpellHeal(event: SpellHealEvent) {
        val spell = event.spell as? HealingSpell ?: return
        val level = getLevel(event.caster) ?: return
        event.amount += (spell.healPerLevel * level).toInt()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onSpellShield(event: SpellShieldEvent) {
        val spell = event.spell as? ShieldingSpell ?: return
        val level = getLevel(event.caster) ?: return
        event.amount += (spell.shieldPerLevel * level).toInt()
    }

    private fun getLevel(player: Player): Int? {
        val gameCharacter = userDataRegistry.getCharacter(player.uniqueId) ?: return null
        return gameCharacter.withSyncCharacterData { traits.level }.takeIf { it > 0 }
    }
}
