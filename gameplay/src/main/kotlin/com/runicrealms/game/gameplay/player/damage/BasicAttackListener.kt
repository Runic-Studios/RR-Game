package com.runicrealms.game.gameplay.player.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import kotlin.math.max

/**
 * Applies weapon attack cooldown after a [BasicAttackEvent] fires.
 *
 * Reads [BasicAttackEvent.cooldownTicks] (which may be modified by other listeners, such as
 * stat scaling) and applies it as a Bukkit item cooldown on the weapon material.
 */
@Singleton
class BasicAttackListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBasicAttack(event: BasicAttackEvent) {
        val ticks = max(BasicAttackEvent.MINIMUM_COOLDOWN_TICKS, event.cooldownTicks.toInt())
        event.player.setCooldown(event.material, ticks)
    }
}
