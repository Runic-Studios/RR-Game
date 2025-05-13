package com.runicrealms.game.data.game

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.runicrealms.trove.client.user.UserCharacterData
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import org.bukkit.plugin.Plugin

/**
 * This class is a wrapper around the GameSession for interaction with a user's player and character
 * data, including reading and staging changes to either.
 */
class GameCharacter internal constructor(plugin: Plugin, gameSession: GameSession) :
    GamePlayer(plugin, gameSession) {

    /**
     * ALL CALLS to withCharacterData MUST be on the Minecraft game thread!
     *
     * Computation can be offloaded to worker threads, but final handling of data must occur on the
     * main thread.
     */
    suspend fun <T> withCharacterData(action: UserCharacterData.() -> T): T {
        val ctx = coroutineContext
        if (ctx[ContinuationInterceptor] != plugin.minecraftDispatcher) {
            throw IllegalStateException("Cannot modify game player data on async thread!")
        }
        return gameSession.characterData!!.action()
    }
}
