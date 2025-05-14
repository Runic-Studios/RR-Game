package com.runicrealms.game.data.game

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.runicrealms.trove.client.user.UserCharacterData
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

/**
 * This class is a wrapper around the GameSession for interaction with a user's player and character
 * data, including reading and staging changes to either.
 */
class GameCharacter internal constructor(plugin: Plugin, gameSession: GameSession) :
    GamePlayer(plugin, gameSession) {

    /**
     * ALL CALLS to withSyncCharacterData MUST be on the Minecraft game thread! (Hence the sync in
     * its name).
     *
     * Computation can be offloaded to worker threads, but final handling of data must occur on the
     * main thread.
     */
    fun <T> withSyncCharacterData(action: UserCharacterData.() -> T): T {
        if (!Bukkit.isPrimaryThread())
            throw IllegalStateException("Cannot modify game player data on async thread!")
        return gameSession.characterData!!.action()
    }

    /**
     * withCharacterData can be called on any thread, but will suspend the context and swap to the
     * main thread if run on an async worker.
     *
     * Computation can be offloaded to worker threads, but final handling of data must occur on the
     * main thread.
     */
    suspend fun <T> withCharacterData(action: UserCharacterData.() -> T): T {
        return withContext(plugin.minecraftDispatcher) {
            return@withContext gameSession.characterData!!.action()
        }
    }
}
