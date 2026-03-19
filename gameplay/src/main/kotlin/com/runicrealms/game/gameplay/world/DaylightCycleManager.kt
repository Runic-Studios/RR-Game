package com.runicrealms.game.gameplay.world

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

private const val DAY_THRESHOLD = 12000L
private const val TICKS_PER_CYCLE = 2L

// Task runs every 2 ticks (100ms). During day, time advances 1 tick per iteration (half speed ->
// 20-minute days). During night, time advances 2 ticks per iteration (normal speed -> ~7-minute nights).
private const val DAY_TICK_ADVANCE = 1L   // TICKS_PER_CYCLE * 0.5
private const val NIGHT_TICK_ADVANCE = 2L // TICKS_PER_CYCLE * 1.0

/**
 * Manages a custom day/night cycle for the Alterra world.
 *
 * - Daytime (tick 0-12000): half speed (20-minute days)
 * - Nighttime (tick 12000+): normal speed (~7-minute nights)
 *
 * Runs on the Minecraft main thread via MCCoroutine to allow safe world mutations.
 */
@Singleton
class DaylightCycleManager
@Inject
constructor(private val plugin: Plugin) {

    init {
        val world = Bukkit.getWorld("Alterra")
        // Reset to day on startup
        world?.time = 0
        startCycleTask()
    }

    private fun startCycleTask() {
        // Switch to the Minecraft main thread for world time mutations.
        plugin.launch {
            while (isActive) {
                delay(TICKS_PER_CYCLE * 50L) // 50ms per tick -> 100ms for 2 ticks
                tick()
            }
        }
    }

    private fun tick() {
        val world = Bukkit.getWorld("Alterra") ?: return
        val time = world.time
        world.time = if (time <= DAY_THRESHOLD) time + DAY_TICK_ADVANCE else time + NIGHT_TICK_ADVANCE
    }
}
