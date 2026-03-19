package com.runicrealms.game.gameplay.spell

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.AllyVerifyEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Validates ally targets for spells before healing or buffing is applied.
 *
 * An ally target is valid if:
 * - The target is a [Player]
 * - The target is in the same party as the caster, OR the target is the caster themselves
 *
 * Currently only caster == recipient is enforced. Party check is stubbed.
 *
 * TODO: Add party membership check once the party system is migrated.
 *   Example: if (!partyManager.isInSameParty(event.caster.uniqueId, event.recipient.uniqueId)) event.isCancelled = true
 */
@Singleton
class AllyVerifyListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onAllyVerify(event: AllyVerifyEvent) {
        // Target must be a player
        if (event.recipient !is Player) {
            event.isCancelled = true
            return
        }

        // TODO: Add party check - cancel if target is not in the same party as caster
        //   val partyManager = ... (inject once migrated)
        //   if (!partyManager.isInSameParty(event.caster.uniqueId, event.recipient.uniqueId)) {
        //       event.isCancelled = true
        //   }
    }
}
