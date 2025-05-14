package com.runicrealms.game.data.game

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.callSuspendingEvent
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterJoinEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.event.GamePlayerDataLoadEvent
import com.runicrealms.game.data.event.GamePlayerJoinEvent
import com.runicrealms.game.data.event.GamePlayerQuitEvent
import com.runicrealms.trove.client.TroveClient
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

/**
 * This is the main worker behind loading and saving a player's data. ALL loading/saving occurs
 * within the class, no exceptions. Player/character data can be read and modified elsewhere, but
 * changes to it are only staged, and the actual saving happens here.
 *
 * This operates on a few principles:
 * - Actual loading/saving DB calls (using Trove) are executed with async dispatcher
 * - Everything else happens on the Minecraft thread. Mostly importantly, this includes adding new
 *   player sessions, adding session characters, ending sessions, etc.
 *
 * This class is responsible for emitting 5 different data-related events:
 * - GamePlayerJoinEvent: Fires SYNCHRONOUSLY after a player joined, and we loaded their data
 * - GamePlayerQuitEvent: Fires SYNCHRONOUSLY after a player has quit, but just before we destroy
 *   their player object and save their data
 * - GameCharacterJoinEvent: Fires SYNCHRONOUSLY after a player has chosen their character, and we
 *   have loaded their data
 * - GameCharacterQuitEvent: Fires SYNCHRONOUSLY after a player has quit or changed characters, but
 *   just before we destroy their player object and save their data
 */
class GameSessionManager
@Inject
constructor(private val troveClient: TroveClient, private val plugin: Plugin) :
    Listener, UserDataRegistry {

    private val logger = LoggerFactory.getLogger("data")

    companion object {
        const val LEASE_EXPIRY_MILLIS: Long = 60 * 1000
        const val SAVE_MILLIS: Long = 30 * 1000
    }

    private val sessions = ConcurrentHashMap<UUID, GameSession>()

    private val players = ConcurrentHashMap<UUID, GamePlayer>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        try {
            val createResult = withContext(plugin.asyncDispatcher) { createSession(event.player) }
            if (!createResult.isSuccess) {
                event.player.kick(
                    Component.text("Failed to load: ${createResult.exceptionOrNull()?.message}")
                )
                logger.error(
                    "Failed to load player ${event.player.name}",
                    IllegalStateException(createResult.exceptionOrNull()!!),
                )
                return
            }
            val session = createResult.getOrNull()!!
            sessions[event.player.uniqueId] = session
            val player = GamePlayer(plugin, session)
            players[event.player.uniqueId] = player

            val playerLoginEvent = GamePlayerJoinEvent(player)

            Bukkit.getPluginManager().callSuspendingEvent(playerLoginEvent, plugin).joinAll()

            if (!playerLoginEvent.success) {
                val unified = playerLoginEvent.errors.map { it.message }.joinToString(", ")
                event.player.kick(Component.text("Failed to load: $unified"))
                for (error in playerLoginEvent.errors) {
                    logger.error("Failed to load player ${event.player.name}", error)
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed to load player ${event.player.name}", exception)
            event.player.kick(Component.text("Failed to load: ${exception.message}"))
        }
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        endSession(event.player.uniqueId, true)
    }

    private fun launchSessionSaveTask(userID: UUID): Job {
        // Any context
        return plugin.launch {
            withContext(plugin.asyncDispatcher) {
                while (true) {
                    val session = sessions[userID] ?: break
                    val saveResult = withContext(NonCancellable) { saveSession(session) }
                    if (!saveResult.isSuccess) {
                        withContext(plugin.minecraftDispatcher) {
                            Bukkit.getPlayer(userID)
                                ?.kick(
                                    Component.text(
                                        saveResult.exceptionOrNull()?.message ?: "Unknown error"
                                    )
                                )
                        }
                        logger.error(
                            "Failed to save session periodically",
                            IllegalStateException(saveResult.exceptionOrNull()!!),
                        )
                        // Note: kicking will trigger a save
                        break
                    }
                    delay(SAVE_MILLIS)
                }
            }
        }
    }

    private suspend fun saveSession(session: GameSession): Result<Unit> {
        // Async context
        val refreshResult = session.claim.refreshLease()
        if (!refreshResult.isSuccess) return refreshResult

        val playerSaveResult = session.playerData.save()
        if (!playerSaveResult.isSuccess) return playerSaveResult

        session.characterMutex.withLock {
            val character = session.characterData ?: return@withLock
            val characterSaveResult = character.save()
            if (!characterSaveResult.isSuccess) return characterSaveResult
        }

        return Result.success(Unit)
    }

    private suspend fun createSession(bukkitPlayer: Player): Result<GameSession> {
        // Async context
        val claimResult = troveClient.createClaim(bukkitPlayer.uniqueId, LEASE_EXPIRY_MILLIS)
        if (!claimResult.isSuccess) {
            return Result.failure(claimResult.exceptionOrNull()!!)
        }
        val claim = claimResult.getOrNull()!!
        val playerResult = claim.loadPlayer()
        if (!playerResult.isSuccess) {
            return Result.failure(playerResult.exceptionOrNull()!!)
        }
        val player = playerResult.getOrNull()!!

        withContext(plugin.minecraftDispatcher) {
            val createEvent = GamePlayerDataLoadEvent(player)
            Bukkit.getPluginManager().callSuspendingEvent(createEvent, plugin).joinAll()
            player.empty = false
        }

        // This will initiate a different-context save when we run it
        val saveTask = launchSessionSaveTask(bukkitPlayer.uniqueId)

        return Result.success(GameSession(claim, player, bukkitPlayer, saveTask))
    }

    // NOTE: assumes you DON'T have a lock on the character data in the calling context!
    override suspend fun setCharacter(user: UUID, slot: Int?): Boolean {
        // Any context
        val session = sessions[user] ?: return false
        return withContext(plugin.asyncDispatcher) {
            session.characterMutex.withLock { // Acquire lock async
                withContext(plugin.minecraftDispatcher) resultContext@{
                    val oldCharacterData = session.characterData ?: return@resultContext true
                    if (slot == oldCharacterData.slot) return@resultContext true
                    if (slot != null) {
                        val creationResult =
                            withContext(plugin.asyncDispatcher) {
                                session.claim.loadCharacter(slot)
                            }
                        if (!creationResult.isSuccess) {
                            logger.error(
                                "Failed to load character session for ${session.bukkitPlayer.uniqueId} slot $slot",
                                IllegalStateException(creationResult.exceptionOrNull()!!),
                            )
                            return@resultContext false
                        }
                        val characterData = creationResult.getOrNull()!!
                        session.characterData = characterData

                        val character = GameCharacter(plugin, session)
                        players[session.bukkitPlayer.uniqueId] = character

                        val characterJoinEvent = GameCharacterJoinEvent(character)
                        Bukkit.getPluginManager()
                            .callSuspendingEvent(characterJoinEvent, plugin)
                            .joinAll()

                        if (!characterJoinEvent.success) {
                            for (error in characterJoinEvent.errors) {
                                logger.error(
                                    "Failed to load character session for ${session.bukkitPlayer.uniqueId} slot $slot",
                                    error,
                                )
                            }
                            return@resultContext false
                        }
                    } else {
                        endCharacterSession(session)
                    }
                    return@resultContext true
                }
            }
        }
    }

    // NOTe: assumes you have a lock on the character data in the calling context!
    private suspend fun endCharacterSession(session: GameSession) {
        // Minecraft game thread context
        val character = players[session.bukkitPlayer.uniqueId] as? GameCharacter ?: return
        val characterQuitEvent = GameCharacterQuitEvent(character)
        Bukkit.getPluginManager().callSuspendingEvent(characterQuitEvent, plugin).joinAll()
        session.characterData = null
        players[session.bukkitPlayer.uniqueId] = GamePlayer(plugin, session)
    }

    private suspend fun endSession(user: UUID, save: Boolean) {
        // Minecraft game thread context
        val session = sessions.remove(user) ?: return

        withContext(plugin.asyncDispatcher) {
            session.saveJob.cancelAndJoin()

            session.characterMutex.withLock {
                withContext(plugin.minecraftDispatcher) { endCharacterSession(session) }
            }
        }

        val player = players[session.bukkitPlayer.uniqueId]!!
        val playerQuitEvent = GamePlayerQuitEvent(player)

        Bukkit.getPluginManager().callSuspendingEvent(playerQuitEvent, plugin).joinAll()

        withContext(plugin.asyncDispatcher) {
            if (save) {
                val saveResult = saveSession(session)
                if (!saveResult.isSuccess) {
                    logger.error(
                        "FATAL: failed to save user $user on session end",
                        IllegalStateException(saveResult.exceptionOrNull()!!),
                    )
                }
            }

            val releaseResult = session.claim.releaseAndClose()
            if (!releaseResult.isSuccess) {
                logger.error(
                    "FATAL: failed to release lock on user $user on session end",
                    IllegalStateException(releaseResult.exceptionOrNull()!!),
                )
            }
        }
    }

    override fun getPlayer(user: UUID): GamePlayer? = players[user]

    override fun getCharacter(user: UUID): GameCharacter? = players[user] as? GameCharacter

    override fun getAllPlayers(): Collection<GamePlayer> = players.values

    override fun getAllCharacters(): Collection<GameCharacter> =
        players.values.filter { it !is GameCharacter }.map { it as GameCharacter }
}
