package com.runicrealms.game.gameplay.spell

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.EnemyVerifyEvent
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Validates enemy targets for spells before damage is applied.
 *
 * A target is invalid (and the event is cancelled) if:
 * - The target is an [ArmorStand]
 * - The target is the caster themselves
 * - The target is a party member of the caster (TODO: party system not yet migrated)
 * - The target is in a safe zone (TODO: safe zone check not yet migrated)
 * - The target is in a dungeon world context that disallows this (TODO)
 */
@Singleton
class EnemyVerifyListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEnemyVerify(event: EnemyVerifyEvent) {
        val victim = event.victim

        // Armor stands are never valid targets
        if (victim is ArmorStand) {
            event.isCancelled = true
            return
        }

        // Cannot target yourself
        if (victim.uniqueId == event.caster.uniqueId) {
            event.isCancelled = true
            return
        }

        // TODO: Cancel if victim is a party member of caster
        //   if (partyManager.isInSameParty(event.caster.uniqueId, victim.uniqueId)) event.isCancelled = true

        // TODO: Cancel if victim (Player) is in a safe zone
        //   if (victim is Player && safeZoneRegistry.isInSafeZone(victim.location)) event.isCancelled = true

        // TODO: Cancel based on dungeon world PvP rules (if applicable)
    }
}
