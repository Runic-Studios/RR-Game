package com.runicrealms.game.gameplay.mob

import com.google.inject.Inject
import com.google.inject.Singleton
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * Removes non-MythicMob living entities from the Alterra world on server shutdown.
 *
 * This cleans up entities left behind by broken disguises (LibsDisguises remnants) that
 * would otherwise persist and clutter the world on next start.
 */
@Singleton
class MobCleanupListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin != plugin) return
        cleanup()
    }

    private fun cleanup() {
        val world = Bukkit.getWorld("Alterra") ?: return
        var removed = 0
        for (entity in world.entities) {
            if (entity !is LivingEntity) continue
            val isMythicMob = MythicBukkit.inst().mobManager.getActiveMob(entity.uniqueId).isPresent
            if (!isMythicMob) {
                entity.remove()
                removed++
            }
        }
        logger.info("MobCleanupListener: removed $removed non-MythicMob entities from Alterra on shutdown")
    }
}
