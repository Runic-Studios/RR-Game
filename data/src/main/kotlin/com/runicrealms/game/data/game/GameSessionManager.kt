package com.runicrealms.game.data.game

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.callSuspendingEvent
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.google.inject.name.Named
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterJoinEvent
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterPreLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.event.GamePlayerJoinEvent
import com.runicrealms.game.data.event.GamePlayerLoadEvent
import com.runicrealms.game.data.event.GamePlayerPreLoadEvent
import com.runicrealms.game.data.event.GamePlayerQuitEvent
import com.runicrealms.game.data.lock.PlayerLockRepository
import com.runicrealms.game.data.model.CharacterData
import com.runicrealms.game.data.model.CharacterTraits
import com.runicrealms.game.data.repository.PlayerRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

/**
 * The core engine for loading, saving, and managing player sessions.
 *
 * ALL loading/saving occurs within this class. Player and character data may be READ and MUTATED by
 * other modules, but persistence is handled exclusively here.
 *
 * Context rules:
 *
 * ASYNC dispatcher ([Plugin.asyncDispatcher]):
 * - All MongoDB calls (lock acquire/renew/release, document load/save).
 * - The periodic save loop body.
 *
 * MC main thread ([Plugin.minecraftDispatcher]):
 * - All Bukkit API calls (teleport, inventory, attribute changes).
 * - All Bukkit event firing (callSuspendingEvent).
 * - All mutations to [sessions] and [players] maps.
 * - Mutations to [GameSession.activeCharacterSlot].
 *
 * Data mutation:
 * - [GameSession.document] fields may be mutated from any thread while holding
 *   [GameSession.dataLock]. The lock is acquired automatically by [GamePlayer.withPlayerData] and
 *   [GameCharacter.withCharacterData] / [GameCharacter.withSyncCharacterData].
 * - The save loop acquires the lock briefly to take a snapshot, then releases before calling
 *   MongoDB, so writers are never blocked for the duration of a DB call.
 *
 * Fail-loud policy:
 *
 * ANY failure in the login flow or periodic save loop MUST kick the player and log at ERROR level.
 * Failures during logout (where we cannot kick) are logged at ERROR level only. No DB failure may
 * fail silently.
 *
 * Session lifecycle:
 * 1. [onPlayerJoin]: acquire lock -> loadOrCreate document -> fire PreLoadEvent -> fire LoadEvent
 *    -> fire JoinEvent -> launch save loop
 * 2. [setCharacter]: pure in-memory: switch slot, fire character events
 * 3. Periodic loop: every 30 s: renew lock, save full document (unconditional)
 * 4. [onPlayerQuit]: cancel save loop -> fire CharacterQuitEvent / PlayerQuitEvent -> final save ->
 *    release lock
 */
class GameSessionManager
@Inject
constructor(
    private val plugin: Plugin,
    private val playerRepository: PlayerRepository,
    private val lockRepository: PlayerLockRepository,
    @Named("serverId") private val serverId: String,
) : Listener, UserDataRegistry {

    private val logger = LoggerFactory.getLogger("data")

    companion object {
        /** How often the periodic save + lock renewal fires. */
        const val SAVE_INTERVAL_MILLIS: Long = 30_000L
    }

    /**
     * Live sessions keyed by player UUID. Mutated on the MC main thread only. ConcurrentHashMap so
     * async readers (periodic save loop) see the current map without requiring a lock: they only
     * read by key, never iterate or remove.
     */
    private val sessions = ConcurrentHashMap<UUID, GameSession>()

    /**
     * Public player/character handles keyed by UUID. Mutated on the MC main thread only. A value is
     * a [GamePlayer] when no character is selected; it is a [GameCharacter] when one is active.
     */
    private val players = ConcurrentHashMap<UUID, GamePlayer>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        val startTime = System.currentTimeMillis()
        try {
            // ASYNC: acquire lock and load document from MongoDB
            val sessionResult = withContext(plugin.asyncDispatcher) { createSession(event.player) }

            if (sessionResult.isFailure) {
                // Lock or load failure -> kick immediately
                val cause = sessionResult.exceptionOrNull()!!
                logger.error("Failed to create session for ${event.player.name}", cause)
                event.player.kick(Component.text("Failed to load your data: ${cause.message}"))
                return
            }

            val session = sessionResult.getOrNull()!!

            // MC THREAD: register session, fire events
            // All map mutations and event firing happen here on the main thread.
            sessions[event.player.uniqueId] = session
            val gamePlayer = GamePlayer(plugin, session)
            players[event.player.uniqueId] = gamePlayer

            // PreLoadEvent: handlers apply first-time defaults if isNewPlayer == true
            val preLoadEvent = GamePlayerPreLoadEvent(event.player.uniqueId, session.document)
            Bukkit.getPluginManager().callSuspendingEvent(preLoadEvent, plugin).joinAll()

            // LoadEvent: handlers apply persisted data to the Bukkit player (e.g. permissions)
            val loadEvent = GamePlayerLoadEvent(gamePlayer)
            Bukkit.getPluginManager().callSuspendingEvent(loadEvent, plugin).joinAll()

            if (!loadEvent.success) {
                val msg = loadEvent.errors.joinToString(", ") { it.message ?: "unknown" }
                logger.error("GamePlayerLoadEvent failed for ${event.player.name}: $msg")
                event.player.kick(Component.text("Failed to load: $msg"))
                // Clean up the partially-registered session
                sessions.remove(event.player.uniqueId)
                players.remove(event.player.uniqueId)
                // Release the lock we just acquired so another server/login attempt can proceed
                plugin.launch {
                    withContext(plugin.asyncDispatcher) {
                        val releaseResult = lockRepository.release(event.player.uniqueId, serverId)
                        if (releaseResult.isFailure) {
                            logger.error(
                                "Failed to release lock after load failure for ${event.player.name}",
                                releaseResult.exceptionOrNull()!!,
                            )
                        }
                    }
                }
                return
            }

            val joinEvent = GamePlayerJoinEvent(gamePlayer)
            Bukkit.getPluginManager().callSuspendingEvent(joinEvent, plugin).joinAll()

            logger.info(
                "Player ${event.player.uniqueId} loaded in " +
                    "${System.currentTimeMillis() - startTime} ms"
            )
        } catch (exception: Exception) {
            logger.error("Unexpected error loading player ${event.player.name}", exception)
            event.player.kick(Component.text("Failed to load: ${exception.message}"))
        }
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        endSession(event.player.uniqueId)
    }

    /**
     * Acquires the distributed lock and loads the player document from MongoDB.
     *
     * Called on the ASYNC dispatcher from [onPlayerJoin].
     *
     * Returns [Result.failure] if the lock cannot be acquired (another server holds it) or if the
     * document load fails.
     */
    private suspend fun createSession(bukkitPlayer: Player): Result<GameSession> {
        // Acquire the distributed lock, retrying if another server still holds it.
        // Players switching servers quickly may arrive here before the previous server has released
        // the lock, so we wait up to 5 seconds for it to be freed.
        var lockResult = lockRepository.acquireOrRenew(bukkitPlayer.uniqueId, serverId)
        if (lockResult.isFailure) {
            logger.warn(
                "Lock is held for player ${bukkitPlayer.name} (${bukkitPlayer.uniqueId}) by " +
                    "another server - retrying up to 5 times"
            )
            for (attempt in 1..5) {
                // Async dispatcher - delay suspends the coroutine without blocking the MC thread
                delay(1_000L)
                lockResult = lockRepository.acquireOrRenew(bukkitPlayer.uniqueId, serverId)
                if (lockResult.isSuccess) {
                    logger.info("Lock acquired for ${bukkitPlayer.name} after $attempt attempt(s)")
                    break
                }
                logger.warn(
                    "Lock still held for player ${bukkitPlayer.name} after attempt $attempt/5"
                )
            }
        }
        if (lockResult.isFailure) {
            return Result.failure(
                IllegalStateException(
                    "Could not acquire data lock for ${bukkitPlayer.uniqueId}. " +
                        "Another server may be handling this player.",
                    lockResult.exceptionOrNull()!!,
                )
            )
        }

        // Load (or create) the player document
        val loadResult = playerRepository.loadOrCreate(bukkitPlayer.uniqueId)
        if (loadResult.isFailure) {
            // Release the lock we just took since we're not going to create a session
            lockRepository.release(bukkitPlayer.uniqueId, serverId)
            return Result.failure(loadResult.exceptionOrNull()!!)
        }

        val document = loadResult.getOrNull()!!

        // Launch the periodic save loop
        // The loop runs entirely on the async dispatcher. It renews the lock and saves the
        // document unconditionally every SAVE_INTERVAL_MILLIS milliseconds.
        val saveJob = launchSaveLoop(bukkitPlayer.uniqueId)

        return Result.success(
            GameSession(
                playerId = bukkitPlayer.uniqueId,
                document = document,
                bukkitPlayer = bukkitPlayer,
                saveJob = saveJob,
            )
        )
    }

    /**
     * Launches a coroutine that periodically:
     * 1. Renews the distributed lock.
     * 2. Saves the full player document to MongoDB.
     *
     * Both operations run on the ASYNC dispatcher. The loop runs until the coroutine is cancelled
     * (which happens in [endSession]).
     *
     * Failure policy: any failure kicks the player and breaks the loop. The subsequent
     * [PlayerQuitEvent] will trigger [endSession], which attempts a final save.
     */
    private fun launchSaveLoop(playerId: UUID): Job {
        return plugin.launch {
            withContext(plugin.asyncDispatcher) {
                // Wait before the first save so we don't immediately double-save on login
                delay(SAVE_INTERVAL_MILLIS)
                while (true) {
                    val session = sessions[playerId] ?: break

                    // Renew the lock
                    val renewResult =
                        withContext(NonCancellable) {
                            lockRepository.acquireOrRenew(playerId, serverId)
                        }
                    if (renewResult.isFailure) {
                        logger.error(
                            "Failed to renew lock for player $playerId: kicking",
                            renewResult.exceptionOrNull()!!,
                        )
                        // Switch to MC thread to kick (Bukkit API requires main thread)
                        withContext(plugin.minecraftDispatcher) {
                            Bukkit.getPlayer(playerId)
                                ?.kick(
                                    Component.text("Data lock renewal failed. Please reconnect.")
                                )
                        }
                        break
                    }

                    // Acquire the data lock briefly to snapshot the document, then release before
                    // calling MongoDB so gameplay is never blocked for the duration of a DB call.
                    val snapshot =
                        withContext(NonCancellable) {
                            session.dataLock.withLock { session.document.copy() }
                        }

                    // Save the snapshot
                    val saveResult = withContext(NonCancellable) { playerRepository.save(snapshot) }
                    if (saveResult.isFailure) {
                        logger.error(
                            "Periodic save failed for player $playerId: kicking",
                            saveResult.exceptionOrNull()!!,
                        )
                        withContext(plugin.minecraftDispatcher) {
                            Bukkit.getPlayer(playerId)
                                ?.kick(
                                    Component.text(
                                        "Data save failed. Please reconnect. Your data is safe."
                                    )
                                )
                        }
                        break
                    }

                    delay(SAVE_INTERVAL_MILLIS)
                }
            }
        }
    }

    /**
     * Tears down the session for [userId]:
     * 1. Cancels and joins the periodic save job (so no concurrent saves happen).
     * 2. Fires [GameCharacterQuitEvent] if a character was active.
     * 3. Fires [GamePlayerQuitEvent].
     * 4. Final save of the full document.
     * 5. Releases the distributed lock.
     *
     * Steps 2–3 happen on the MC main thread; steps 1, 4–5 on the async dispatcher.
     *
     * Failures during steps 4–5 are logged at ERROR but cannot kick (player already quit).
     */
    private suspend fun endSession(userId: UUID) {
        val startTime = System.currentTimeMillis()
        val session = sessions.remove(userId) ?: return

        // ASYNC: cancel the periodic save job before doing anything else
        // cancelAndJoin ensures any in-flight save finishes before we proceed.
        withContext(plugin.asyncDispatcher) { session.saveJob.cancelAndJoin() }

        // MC THREAD: fire quit events
        // Fire CharacterQuitEvent first so game systems can serialise character state
        // (e.g. location, inventory) back into the in-memory document before we save.
        val activeSlot = session.activeCharacterSlot
        if (activeSlot != null) {
            val character = players[userId] as? GameCharacter
            if (character != null) {
                val charQuitEvent = GameCharacterQuitEvent(character, isOnLogout = true)
                Bukkit.getPluginManager().callSuspendingEvent(charQuitEvent, plugin).joinAll()
            }
        }

        val player = players.remove(userId)
        if (player != null) {
            val playerQuitEvent = GamePlayerQuitEvent(player)
            Bukkit.getPluginManager().callSuspendingEvent(playerQuitEvent, plugin).joinAll()
        }

        // ASYNC: final save + lock release
        withContext(plugin.asyncDispatcher) {
            // Snapshot under the data lock so any last-moment mutations from quit events are
            // captured, then release the lock before the MongoDB call.
            val snapshot = session.dataLock.withLock { session.document.copy() }
            val saveResult = playerRepository.save(snapshot)
            if (saveResult.isFailure) {
                // Cannot kick: player already disconnected. Log at ERROR
                logger.error(
                    "FATAL: final save failed for player $userId on logout",
                    saveResult.exceptionOrNull()!!,
                )
            }

            val releaseResult = lockRepository.release(userId, serverId)
            if (releaseResult.isFailure) {
                // Stale lock will expire via TTL, but log so we can investigate.
                logger.error(
                    "FATAL: failed to release lock for player $userId on logout",
                    releaseResult.exceptionOrNull()!!,
                )
            }
        }

        logger.info(
            "Session ended for player $userId in ${System.currentTimeMillis() - startTime} ms"
        )
    }

    /**
     * Switches [user] to character [slot], or clears the active character if [slot] is null.
     *
     * Must be called on the MC main thread (event firing requires it).
     */
    override suspend fun setCharacter(user: UUID, slot: Int?): Boolean {
        val startTime = System.currentTimeMillis()
        val session = sessions[user] ?: return false

        // Fire quit event for the previously active character (if any)
        val previousSlot = session.activeCharacterSlot
        if (previousSlot != null) {
            val previousCharacter = players[user] as? GameCharacter
            if (previousCharacter != null) {
                val quitEvent = GameCharacterQuitEvent(previousCharacter, isOnLogout = false)
                Bukkit.getPluginManager().callSuspendingEvent(quitEvent, plugin).joinAll()
            }
            // Downgrade the player handle back to a plain GamePlayer
            players[user] = GamePlayer(plugin, session)
        }

        if (slot == null) {
            // Player is returning to character selection: no further work needed
            session.activeCharacterSlot = null
            return true
        }

        // Resolve or create the CharacterData for this slot
        val existingData = session.document.characters[slot.toString()]
        val characterData: CharacterData =
            if (existingData != null) {
                existingData
            } else {
                // New character: create a default and insert it directly into the document map.
                val newData = playerRepository.defaultCharacterData(slot)
                session.document.characters += (slot.toString() to newData)
                newData
            }

        // PreLoadEvent: handlers apply first-time class defaults (e.g. classType, start location)
        val preLoadEvent = GameCharacterPreLoadEvent(user, characterData, slot)
        Bukkit.getPluginManager().callSuspendingEvent(preLoadEvent, plugin).joinAll()

        // Update active slot before constructing GameCharacter so resolveCharacterData works
        session.activeCharacterSlot = slot

        val gameCharacter = GameCharacter(plugin, session, slot)
        players[user] = gameCharacter

        // LoadEvent: handlers apply persisted data to the Bukkit player
        val loadEvent = GameCharacterLoadEvent(gameCharacter)
        Bukkit.getPluginManager().callSuspendingEvent(loadEvent, plugin).joinAll()

        if (!loadEvent.success) {
            val msg = loadEvent.errors.joinToString(", ") { it.message ?: "unknown" }
            logger.error("GameCharacterLoadEvent failed for player $user slot $slot: $msg")
            // Revert to no active character
            session.activeCharacterSlot = null
            players[user] = GamePlayer(plugin, session)
            return false
        }

        // JoinEvent: character is fully ready
        val joinEvent = GameCharacterJoinEvent(gameCharacter)
        Bukkit.getPluginManager().callSuspendingEvent(joinEvent, plugin).joinAll()

        logger.info(
            "Character slot $slot loaded for player $user in " +
                "${System.currentTimeMillis() - startTime} ms"
        )
        return true
    }

    override fun getPlayer(user: UUID): GamePlayer? = players[user]

    override fun getCharacter(user: UUID): GameCharacter? = players[user] as? GameCharacter

    override fun getAllPlayers(): Collection<GamePlayer> = players.values

    override fun getAllCharacters(): Collection<GameCharacter> =
        players.values.filterIsInstance<GameCharacter>()

    /**
     * Returns the [CharacterTraits] for every character slot belonging to [user].
     *
     * No DB call: the full document (with all characters) was loaded at login. Returns null if the
     * player is not currently online.
     */
    override fun loadUserCharactersTraits(user: UUID): Map<Int, CharacterTraits>? {
        val session = sessions[user] ?: return null
        return session.document.characters
            .mapValues { (_, charData) -> charData.traits }
            .mapKeys { (key, _) -> key.toInt() }
    }

    /**
     * Kicks all online players, fires quit events to serialise their state, then synchronously
     * saves all sessions and releases all distributed locks.
     *
     * Called from [com.runicrealms.game.plugin.GamePlugin.onDisable] to ensure a clean shutdown.
     * During normal play, [onPlayerQuit] handles teardown per-player via a coroutine. However, when
     * the server stops, Bukkit fires [PlayerQuitEvent] for every online player but MCCoroutine
     * cancels the plugin's coroutine scope before those async handlers finish, leaving locks
     * unreleased and quit-event state serialisation (location, inventory, etc.) incomplete.
     * This method runs synchronously (via [runBlocking]) so everything is guaranteed to complete
     * before [onDisable] returns.
     *
     * Each session is removed from [sessions] before kicking the player, so any [endSession]
     * coroutine launched from the resulting [PlayerQuitEvent] will find no session and return
     * early - there is no double-processing.
     *
     * Quit events are dispatched synchronously via [org.bukkit.plugin.PluginManager.callEvent]
     * rather than [com.github.shynixn.mccoroutine.bukkit.callSuspendingEvent] because the
     * plugin's coroutine scope may already be tearing down during [onDisable]. All current quit
     * event handlers are non-suspending so synchronous dispatch is sufficient.
     */
    fun shutdown() {
        val sessionIds = sessions.keys.toList()
        if (sessionIds.isEmpty()) return
        logger.info("Shutdown: saving ${sessionIds.size} active session(s)...")
        runBlocking {
            for (userId in sessionIds) {
                // Remove from the sessions map first so any endSession() coroutine launched
                // from the kick's PlayerQuitEvent finds no session and returns early.
                val session = sessions.remove(userId) ?: continue

                Bukkit.getPlayer(userId)?.kick(
                    Component.text("Server is shutting down. Please reconnect shortly.", NamedTextColor.RED)
                )

                // Fire GameCharacterQuitEvent so handlers can serialise character state
                // (location, inventory, etc.) back into the in-memory document before we save.
                val activeSlot = session.activeCharacterSlot
                if (activeSlot != null) {
                    val character = players[userId] as? GameCharacter
                    if (character != null) {
                        Bukkit.getPluginManager()
                            .callSuspendingEvent(GameCharacterQuitEvent(character, isOnLogout = true), plugin).joinAll()
                    }
                }

                // Remove the player handle after events so handlers can still call
                // getCharacter/getPlayer during event processing.
                val player = players.remove(userId)
                if (player != null) {
                    Bukkit.getPluginManager().callSuspendingEvent(GamePlayerQuitEvent(player), plugin).joinAll()
                }

                // Cancel the periodic save loop and wait for any in-flight NonCancellable save
                // to finish before we take our own snapshot.
                session.saveJob.cancelAndJoin()

                // Use Dispatchers.IO directly: plugin.asyncDispatcher may be unavailable once
                // MCCoroutine starts tearing down the plugin's coroutine session.
                withContext(Dispatchers.IO) {
                    val snapshot = session.dataLock.withLock { session.document.copy() }
                    val saveResult = playerRepository.save(snapshot)
                    if (saveResult.isFailure) {
                        logger.error(
                            "FATAL: shutdown save failed for player $userId",
                            saveResult.exceptionOrNull()!!,
                        )
                    }
                    val releaseResult = lockRepository.release(userId, serverId)
                    if (releaseResult.isFailure) {
                        logger.error(
                            "FATAL: shutdown lock release failed for player $userId",
                            releaseResult.exceptionOrNull()!!,
                        )
                    }
                }
            }
        }
        logger.info("Shutdown: all sessions saved and locks released")
    }
}
