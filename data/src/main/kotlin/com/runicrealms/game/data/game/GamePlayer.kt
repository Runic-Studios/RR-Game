package com.runicrealms.game.data.game

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.runicrealms.trove.client.user.UserPlayerData
import org.bukkit.plugin.Plugin
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/**
 * This class is a wrapper around the GameSession for interaction with
 * a user's player data, including reading and staging changes to it.
 */
open class GamePlayer internal constructor(
    protected val plugin: Plugin,
    internal val gameSession: GameSession
) {

    val player = gameSession.bukkitPlayer

    /**
     * ALL CALLS to withPlayerData MUST be on the Minecraft game thread!
     *
     * Computation can be offloaded to worker threads, but final handling of data must occur on this thread.
     */
    suspend fun <T> withPlayerData(action: UserPlayerData.() -> T): T {
        val ctx = coroutineContext
        if (ctx[ContinuationInterceptor] != plugin.minecraftDispatcher) {
            throw IllegalStateException("Cannot modify game player data on async thread!")
        }
        return gameSession.playerData.action()
    }

}