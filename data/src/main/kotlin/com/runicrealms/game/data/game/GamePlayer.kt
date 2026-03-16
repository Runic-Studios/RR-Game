package com.runicrealms.game.data.game

import com.runicrealms.game.data.model.PlayerData
import com.runicrealms.game.data.model.PlayerDocument
import org.bukkit.plugin.Plugin

/**
 * Public wrapper around a player session that exposes player-level data for reading and mutation by
 * other modules.
 *
 * Thread-safety contract:
 * - [withPlayerData] may be called from any coroutine context or thread. It acquires the per-player
 *   [PlayerDataLock] before running [action], guaranteeing mutual exclusion with the save loop and
 *   any other concurrent data accessor.
 * - Because the lock is reentrant within the same coroutine, it is safe to call [withPlayerData]
 *   from inside a [com.runicrealms.game.data.game.GameCharacter.withCharacterData] block and vice
 *   versa without deadlocking.
 * - Bukkit API calls (teleport, inventory, attribute changes) still require the Minecraft main
 *   thread and must be wrapped in withContext(plugin.minecraftDispatcher) as needed.
 */
open class GamePlayer
internal constructor(protected val plugin: Plugin, internal val gameSession: GameSession) {

    val bukkitPlayer = gameSession.bukkitPlayer

    /**
     * Acquires the per-player data lock, then provides mutable access to the player's [PlayerData]
     * for the duration of [action].
     *
     * Safe to call from any thread or coroutine context. Suspends until the lock is available.
     */
    suspend fun <T> withPlayerData(action: suspend PlayerData.() -> T): T =
        gameSession.dataLock.withLock { gameSession.document.player.action() }

    /**
     * Acquires the per-player data lock and runs [action] with the full [PlayerDocument] as the
     * receiver.
     *
     * Use this when you need to access parts of the document that are not under [PlayerData] (e.g.
     * the [com.runicrealms.game.data.model.PlayerDocument.characters] map). Prefer [withPlayerData]
     * or [com.runicrealms.game.data.game.GameCharacter.withCharacterData] where possible, as they
     * are more focused.
     */
    suspend fun <T> withDocument(action: suspend PlayerDocument.() -> T): T =
        gameSession.dataLock.withLock { gameSession.document.action() }
}
