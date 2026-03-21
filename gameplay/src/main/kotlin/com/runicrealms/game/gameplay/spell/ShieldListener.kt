package com.runicrealms.game.gameplay.spell

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.gameplay.spell.event.EnvironmentDamageEvent
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.RunicDamageEvent
import com.runicrealms.game.gameplay.spell.event.ShieldBreakEvent
import com.runicrealms.game.gameplay.spell.spelltypes.ShieldPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Manages the player shield absorption system.
 *
 * Shield state is owned by [SpellManager.shieldedPlayers] and populated by
 * [SpellManager.shieldPlayer]. This listener intercepts all incoming damage events for shielded
 * players, absorbs damage from the active shield, and fires [ShieldBreakEvent] when the shield is
 * depleted or expires.
 *
 * Expiry is checked every 500 ms via a coroutine loop. Shield storage requires no character data
 * field - shields are transient in-memory state cleared on logout.
 */
@Singleton
class ShieldListener
@Inject
constructor(private val plugin: Plugin, private val spellManager: SpellManager) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        startExpiryLoop()
    }

    // --- Damage absorption ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        absorbRunicDamage(event.victim as? Player ?: return, event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        absorbRunicDamage(event.victim as? Player ?: return, event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        absorbRunicDamage(event.victim as? Player ?: return, event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEnvironmentDamage(event: EnvironmentDamageEvent) {
        val payload = spellManager.getShieldedPlayers()[event.player.uniqueId] ?: return
        val absorbed = minOf(payload.shield.amount, event.damage)
        payload.shield.amount -= absorbed
        event.damage -= absorbed
        if (payload.shield.amount <= 0) {
            Bukkit.getPluginManager()
                .callEvent(ShieldBreakEvent(payload, ShieldBreakEvent.BreakReason.DAMAGE))
        }
    }

    // --- Shield break ---

    @EventHandler(priority = EventPriority.NORMAL)
    fun onShieldBreak(event: ShieldBreakEvent) {
        val player = event.shieldPayload.player
        spellManager.removeShield(player.uniqueId)
        player.sendActionBar(Component.text("Your shield has broken!", NamedTextColor.RED))
    }

    // --- Cleanup on logout ---

    @EventHandler
    fun onCharacterQuit(event: GameCharacterQuitEvent) {
        spellManager.removeShield(event.character.bukkitPlayer.uniqueId)
    }

    // --- Internal ---

    private fun absorbRunicDamage(player: Player, event: RunicDamageEvent) {
        val payload = spellManager.getShieldedPlayers()[player.uniqueId] ?: return
        val absorbed = minOf(payload.shield.amount, event.amount.toDouble()).toInt()
        payload.shield.amount -= absorbed
        event.amount -= absorbed
        if (payload.shield.amount <= 0) {
            Bukkit.getPluginManager()
                .callEvent(ShieldBreakEvent(payload, ShieldBreakEvent.BreakReason.DAMAGE))
        }
    }

    /** Fires [ShieldBreakEvent] for any shield that has passed its expiry time. */
    private fun startExpiryLoop() {
        plugin.launch {
            while (isActive) {
                delay(500L)
                val expired: List<ShieldPayload> =
                    spellManager.getShieldedPlayers().values.filter { it.shield.isExpired() }
                for (payload in expired) {
                    Bukkit.getPluginManager()
                        .callEvent(ShieldBreakEvent(payload, ShieldBreakEvent.BreakReason.FALLOFF))
                }
            }
        }
    }
}
