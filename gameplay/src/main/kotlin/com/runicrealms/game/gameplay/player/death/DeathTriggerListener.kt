package com.runicrealms.game.gameplay.player.death

import com.google.inject.Inject
import com.runicrealms.game.gameplay.spell.event.RunicDeathEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin

/**
 * Intercepts vanilla damage events to detect when a player's health would reach 0. When that
 * occurs, the vanilla event is cancelled (preventing the default death/respawn screen) and a
 * [RunicDeathEvent] is fired with the victim and optional killer instead.
 */
class DeathTriggerListener @Inject constructor(plugin: Plugin) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    /**
     * Handles entity-by-entity damage (melee, arrows, spells). Runs at LOWEST so custom damage
     * events have already set their final amounts before we check for death.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        if (victim.health - event.finalDamage > 0) return

        event.isCancelled = true

        // Resolve arrow shooter so the death message names the correct attacker
        val damager: Entity? =
            if (event.damager is Arrow) {
                val shooter = (event.damager as Arrow).shooter
                if (shooter is Entity) shooter else null
            } else {
                event.damager
            }

        Bukkit.getPluginManager().callEvent(RunicDeathEvent(victim, victim.location, damager))
    }

    /**
     * Handles environmental damage (fall, fire, drowning, etc.). Runs at NORMAL priority so it does
     * not conflict with the entity-by-entity handler above.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityDamage(event: EntityDamageEvent) {
        // Avoid double-handling: the above handler already covers EntityDamageByEntityEvent
        if (event is EntityDamageByEntityEvent) return

        val victim = event.entity as? Player ?: return
        if (victim.health - event.finalDamage > 0) return

        event.isCancelled = true
        Bukkit.getPluginManager().callEvent(RunicDeathEvent(victim, victim.location))
    }
}
