package com.runicrealms.game.gameplay.player

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.items.loot.BossTimedLootManager
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Forwards player damage events to [com.runicrealms.game.items.loot.BossTimedLootManager] so it can track per-player
 * damage contribution for boss loot eligibility thresholds.
 *
 * This listener exists in the gameplay module because the damage events ([PhysicalDamageEvent],
 * [MagicDamageEvent]) are defined in the gameplay module and the loot tracking sits in items.
 * Running this listener here avoids adding a items -> gameplay module dependency.
 */
@Singleton
class BossTimedLootDamageListener
@Inject
constructor(
    private val plugin: Plugin,
    private val bossTimedLootManager: BossTimedLootManager,
) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (event.isCancelled) return
        val victim = event.victim as? LivingEntity ?: return
        bossTimedLootManager.trackBossDamage(event.caster, victim, event.amount)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onMagicDamage(event: MagicDamageEvent) {
        if (event.isCancelled) return
        val victim = event.victim as? LivingEntity ?: return
        bossTimedLootManager.trackBossDamage(event.caster, victim, event.amount)
    }
}