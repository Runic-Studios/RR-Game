package com.runicrealms.game.gameplay.spell.spelltypes

import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/**
 * Tracks an [AtomicInteger] stack count and a [BukkitTask] cleanup timer for stacking mechanics.
 * Registers itself with [StackTaskRegistry] on construction.
 */
class StackTask(
    val caster: Player,
    val spell: Spell,
    val stacks: AtomicInteger,
    private var bukkitTask: BukkitTask,
    private val registry: StackTaskRegistry,
) {

    init {
        registry.registerStackTask(this)
    }

    /** Cancels the current decay task and reschedules it with a new [cleanupTask]. */
    fun reset(durationTicks: Long, cleanupTask: Runnable) {
        bukkitTask.cancel()
        bukkitTask =
            caster.server.scheduler.runTaskLater(
                caster.server.pluginManager.plugins[0],
                cleanupTask,
                durationTicks,
            )
    }

    fun cancelTask() {
        bukkitTask.cancel()
        registry.unregisterStackTask(this)
    }

    fun getBukkitTask(): BukkitTask = bukkitTask
}
