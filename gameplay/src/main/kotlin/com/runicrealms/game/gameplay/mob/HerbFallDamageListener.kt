package com.runicrealms.game.gameplay.mob

import com.google.inject.Inject
import com.google.inject.Singleton
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin

/**
 * Prevents fall damage for MythicMobs entities in the "herb" faction.
 *
 * Herb-faction mobs are passive resource nodes that should not take fall damage when spawned or
 * moved by world mechanics.
 *
 * TODO: Verify that the faction name is exactly "herb" in the MythicMobs configuration.
 */
@Singleton
class HerbFallDamageListener @Inject constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityFallDamage(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return

        val optional = MythicBukkit.inst().mobManager.getActiveMob(event.entity.uniqueId)
        if (!optional.isPresent) return

        val activeMob = optional.get()
        // TODO: Verify faction name "herb" against MythicMobs config
        if (activeMob.faction.equals("herb", ignoreCase = true)) {
            event.isCancelled = true
        }
    }
}
