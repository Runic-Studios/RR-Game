package com.runicrealms.game.gameplay.player.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.EnvironmentDamageEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin

private val ENVIRONMENT_CAUSES = setOf(
    EntityDamageEvent.DamageCause.FALL,
    EntityDamageEvent.DamageCause.FIRE,
    EntityDamageEvent.DamageCause.FIRE_TICK,
    EntityDamageEvent.DamageCause.LAVA,
    EntityDamageEvent.DamageCause.DROWNING,
    EntityDamageEvent.DamageCause.SUFFOCATION,
    EntityDamageEvent.DamageCause.VOID,
    EntityDamageEvent.DamageCause.LIGHTNING,
    EntityDamageEvent.DamageCause.POISON,
    EntityDamageEvent.DamageCause.WITHER,
    EntityDamageEvent.DamageCause.HOT_FLOOR,
    EntityDamageEvent.DamageCause.FREEZE,
    EntityDamageEvent.DamageCause.STARVATION,
)

/**
 * Intercepts vanilla environment damage for players and routes it through [EnvironmentDamageEvent].
 *
 * This combines two responsibilities from the old system:
 * - EnvironmentDamageListener: cancels vanilla [EntityDamageEvent] and fires the custom event
 * - GenericDamageListener: listens to [EnvironmentDamageEvent] and applies the final damage
 */
@Singleton
class EnvironmentDamageListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /**
     * Intercepts environment damage at LOWEST priority (before all other handlers) so that
     * downstream listeners receive the custom event instead of the vanilla one.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!ENVIRONMENT_CAUSES.contains(event.cause)) return

        // Cancel vanilla damage and fire our custom event instead
        event.isCancelled = true
        val customEvent = EnvironmentDamageEvent(player, event.cause, event.damage)
        plugin.server.pluginManager.callEvent(customEvent)
    }

    /** Applies environment damage after all modifier listeners have had a chance to adjust it. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEnvironmentDamage(event: EnvironmentDamageEvent) {
        if (event.isCancelled) return
        if (event.damage <= 0) return
        val player = event.player
        player.noDamageTicks = 0
        player.damage(event.damage)
    }
}
