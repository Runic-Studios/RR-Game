package com.runicrealms.game.data.game

import com.runicrealms.game.data.model.CharacterData
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.Plugin

/**
 * Extends [GamePlayer] with access to the currently active character's [CharacterData].
 *
 * A [GameCharacter] exists in the [GameSessionManager.players] map only while a player has an
 * active character selected. It is replaced with a plain [GamePlayer] when the player returns to
 * the character selection screen.
 *
 * Thread-safety contract:
 * - Both accessors acquire the per-player [PlayerDataLock] before running [action], guaranteeing
 *   mutual exclusion with the save loop and any other concurrent accessor.
 * - The lock is reentrant: calling [withSyncCharacterData] or [withCharacterData] from inside
 *   another [withCharacterData] or [withPlayerData] block in the same coroutine does not deadlock.
 * - [withSyncCharacterData] is intended for synchronous Bukkit event handlers (plain Java threads).
 *   It uses [runBlocking] to bridge the synchronous call-site into the coroutine world. Do NOT call
 *   it from inside a coroutine that may already be holding the lock on a different code path, as
 *   that risks deadlock; use [withCharacterData] (suspending) from coroutines instead.
 * - Bukkit API calls (teleport, inventory, attributes) still require the Minecraft main thread.
 */
class GameCharacter internal constructor(plugin: Plugin, gameSession: GameSession, val slot: Int) :
    GamePlayer(plugin, gameSession) {

    /**
     * Synchronous character data accessor for use on the Minecraft main thread in plain (non-
     * suspending) event handlers.
     *
     * Acquires the per-player data lock via [runBlocking]. Only call this from a synchronous
     * context (Bukkit event handler, not from inside a coroutine). For coroutine contexts use
     * [withCharacterData] instead.
     */
    fun <T> withSyncCharacterData(action: CharacterData.() -> T): T = runBlocking {
        gameSession.dataLock.withLock { resolveCharacterData().action() }
    }

    /**
     * Suspending character data accessor for use from coroutines.
     *
     * Acquires the per-player data lock, then runs [action] with the [CharacterData] as receiver.
     * Suspends until the lock is available. Safe to call from any dispatcher.
     */
    suspend fun <T> withCharacterData(action: suspend CharacterData.() -> T): T =
        gameSession.dataLock.withLock { resolveCharacterData().action() }

    /**
     * Retrieves the [CharacterData] for this slot from the in-memory document. Guaranteed non-null:
     * a [GameCharacter] is only constructed after the slot is confirmed to exist in
     * [GameSession.document].characters.
     */
    private fun resolveCharacterData(): CharacterData =
        checkNotNull(gameSession.document.characters[slot.toString()]) {
            "CharacterData for slot $slot not found in document for ${gameSession.playerId}. " +
                "This is a bug: GameCharacter should never be constructed for a missing slot."
        }
}
