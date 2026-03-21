package com.runicrealms.game.gameplay.spell.spelltypes

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.coroutines.delay
import org.bukkit.plugin.Plugin

/**
 * Global registry of active [StackTask] objects. Runs a periodic coroutine to evict tasks whose
 * player is offline.
 */
@Singleton
class StackTaskRegistry @Inject constructor(private val plugin: Plugin) {

    private val stackTasks: MutableList<StackTask> = mutableListOf()

    init {
        plugin.launch {
            while (true) {
                // Evict tasks for offline players
                val toRemove = stackTasks.filter { !it.caster.isOnline }
                toRemove.forEach { task ->
                    task.cancelTask()
                    stackTasks.remove(task)
                }
                delay(CLEANUP_INTERVAL_MS)
            }
        }
    }

    fun registerStackTask(task: StackTask) {
        stackTasks.add(task)
    }

    fun unregisterStackTask(task: StackTask) {
        stackTasks.remove(task)
    }

    private companion object {
        const val CLEANUP_INTERVAL_MS = 1000L
    }
}
