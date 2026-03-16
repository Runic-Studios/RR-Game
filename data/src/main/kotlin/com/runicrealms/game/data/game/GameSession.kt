package com.runicrealms.game.data.game

import com.runicrealms.game.data.model.PlayerDocument
import java.util.UUID
import kotlinx.coroutines.Job
import org.bukkit.entity.Player

/**
 * Holds the complete runtime state for one online player session.
 *
 * This class is internal to the data module. No code outside this module should interact with it
 * directly; use [GamePlayer] / [GameCharacter] as the public API instead.
 *
 * Thread-safety contract:
 * - [document] fields may be mutated from any thread or coroutine, BUT the caller must hold the
 *   [dataLock] while doing so. The lock is acquired automatically by [GamePlayer.withPlayerData]
 *   and [GameCharacter.withCharacterData] / [GameCharacter.withSyncCharacterData]; callers never
 *   acquire it manually.
 * - [activeCharacterSlot] is still mutated only on the Minecraft main thread (by
 *   [GameSessionManager.setCharacter]), where no concurrent writes occur.
 * - The [saveJob] coroutine acquires [dataLock] to take a snapshot before saving; it never holds
 *   the lock across the MongoDB call itself.
 */
internal data class GameSession(
    val playerId: UUID,
    /** Full player aggregate: all characters included. Mutate only while holding [dataLock]. */
    var document: PlayerDocument,
    val bukkitPlayer: Player,
    /**
     * Coroutine job that runs the periodic save loop (async dispatcher). Cancelled and joined
     * during [GameSessionManager.endSession].
     */
    val saveJob: Job,
    /** Per-player reentrant lock. Acquired automatically by all data accessor functions. */
    val dataLock: PlayerDataLock = PlayerDataLock(),
    /**
     * The currently active character slot, or null if the player is on the character selection
     * screen. Mutated on the Minecraft main thread only.
     */
    var activeCharacterSlot: Int? = null,
)
