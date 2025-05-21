package com.runicrealms.game.data.game

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.callSuspendingEvent
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterJoinEvent
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterPreLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.event.GamePlayerJoinEvent
import com.runicrealms.game.data.event.GamePlayerLoadEvent
import com.runicrealms.game.data.event.GamePlayerPreLoadEvent
import com.runicrealms.game.data.event.GamePlayerQuitEvent
import com.runicrealms.trove.client.TroveClient
import com.runicrealms.trove.client.user.UserCharactersTraits
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
 * This class is responsible for emitting several different data-related events:
 * - GamePlayerPreLoadEvent: Fires SYNCHRONOUSLY after a player joined, and we loaded their data
 *   This should be used for checking to see if it is a new player (first login), and adding default
 *   data values if necessary. This event can't be failed.
 * - GamePlayerLoadEvent: Fires SYNCHRONOUSLY after a player joined, and we loaded their data This
 *   should be used for applying the data we have loaded on to the bukkit player This event can be
 *   "failed".
 * - GamePlayerJoinEvent: Fires SYNCHRONOUSLY after a player joined, and we loaded their data, and
 *   after GamePlayerLoadEvent. This event cannot fail.
 * - GamePlayerQuitEvent: Fires SYNCHRONOUSLY after a player has quit, but just before we destroy
 *   their player object and save their data
 * - GameCharacterPreLoadEvent: Fires SYNCHRONOUSLY after a player has chosen their character, and
 *   we have loaded their data. This should be used for checking to see if it is a new character
 *   (just created), and adding default data values if necessary. This event can't be failed.
 * - GameCharacterLoadEvent: Fires SYNCHRONOUSLY after a player has chosen their character, and we
 *   have loaded their data. This should be used for applying the data we have loaded on to the
 *   bukkit player (e.g. inventory, etc). This event can be "failed".
 * - GameCharacterJoinEvent: Fires SYNCHRONOUSLY after a player has chosen their character and we
 *   have fired GameCharacterLoadEvent successfully. This event cannot fail.
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
        val startTime = System.currentTimeMillis()
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

            val playerLoginEvent = GamePlayerLoadEvent(player)

            Bukkit.getPluginManager().callSuspendingEvent(playerLoginEvent, plugin).joinAll()

            if (!playerLoginEvent.success) {
                val unified = playerLoginEvent.errors.map { it.message }.joinToString(", ")
                event.player.kick(Component.text("Failed to load: $unified"))
                for (error in playerLoginEvent.errors) {
                    logger.error("Failed to load player ${event.player.name}", error)
                }
            }

            val playerJoinEvent = GamePlayerJoinEvent(player)
            Bukkit.getPluginManager().callSuspendingEvent(playerJoinEvent, plugin).joinAll()

            val time = System.currentTimeMillis() - startTime
            logger.info(
                "Finished pre-load/load/join for player ${event.player.uniqueId} in $time millis"
            )
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
            if (players[session.bukkitPlayer.uniqueId] !is GameCharacter) {
                session.characterData = null
            }
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
            val preLoadEvent = GamePlayerPreLoadEvent(bukkitPlayer.uniqueId, player)
            Bukkit.getPluginManager().callSuspendingEvent(preLoadEvent, plugin).joinAll()
            player.empty = false
        }

        // This will initiate a different-context save when we run it
        val saveTask = launchSessionSaveTask(bukkitPlayer.uniqueId)

        return Result.success(GameSession(claim, player, bukkitPlayer, saveTask))
    }

    // NOTE: assumes you DON'T have a lock on the character data in the calling context!
    override suspend fun setCharacter(user: UUID, slot: Int?): Boolean {
        // Any context
        val startTime = System.currentTimeMillis()
        val session = sessions[user] ?: return false
        return withTimeout(5000) {
            withContext(plugin.asyncDispatcher) {
                session.characterMutex.withLock { // Acquire lock async
                    withContext(plugin.minecraftDispatcher) resultContext@{
                        val oldCharacterData = session.characterData
                        if (slot == oldCharacterData?.slot) return@resultContext true
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

                            val preLoadEvent = GameCharacterPreLoadEvent(user, characterData)
                            Bukkit.getPluginManager()
                                .callSuspendingEvent(preLoadEvent, plugin)
                                .joinAll()
                            characterData.empty = false

                            session.characterData = characterData

                            val character = GameCharacter(plugin, session, slot)
                            players[session.bukkitPlayer.uniqueId] = character

                            val characterLoadEvent = GameCharacterLoadEvent(character)
                            Bukkit.getPluginManager()
                                .callSuspendingEvent(characterLoadEvent, plugin)
                                .joinAll()

                            if (!characterLoadEvent.success) {
                                for (error in characterLoadEvent.errors) {
                                    logger.error(
                                        "Failed to load character session for ${session.bukkitPlayer.uniqueId} slot $slot",
                                        error,
                                    )
                                }
                                return@resultContext false
                            }

                            val characterJoinEvent = GameCharacterJoinEvent(character)
                            Bukkit.getPluginManager()
                                .callSuspendingEvent(characterJoinEvent, plugin)
                                .joinAll()

                            val time = System.currentTimeMillis() - startTime
                            logger.info(
                                "Finished pre-load/load/join for character $slot of player ${session.bukkitPlayer.uniqueId} in $time millis"
                            )
                        } else {
                            endCharacterSession(session, false)
                        }
                        return@resultContext true
                    }
                }
            }
        }
    }

    // NOTe: assumes you have a lock on the character data in the calling context!
    private suspend fun endCharacterSession(session: GameSession, isOnLogout: Boolean) {
        // Minecraft game thread context
        val character = players[session.bukkitPlayer.uniqueId] as? GameCharacter ?: return
        val characterQuitEvent = GameCharacterQuitEvent(character, isOnLogout)
        Bukkit.getPluginManager().callSuspendingEvent(characterQuitEvent, plugin).joinAll()
        players[session.bukkitPlayer.uniqueId] = GamePlayer(plugin, session)
        // NOTE: doesn't set character data to null! Responsibility of the calling method to do so
        // (after saving)
    }

    private suspend fun endSession(user: UUID, save: Boolean) {
        // Minecraft game thread context
        val startTime = System.currentTimeMillis()
        val session = sessions.remove(user) ?: return
        withContext(plugin.asyncDispatcher) {
            session.saveJob.cancelAndJoin()

            session.characterMutex.withLock {
                withContext(plugin.minecraftDispatcher) { endCharacterSession(session, true) }
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
        val time = System.currentTimeMillis() - startTime
        logger.info(
            "Finished unload/quit/save for player ${session.bukkitPlayer.uniqueId} in $time millis"
        )
    }

    override fun getPlayer(user: UUID): GamePlayer? = players[user]

    override fun getCharacter(user: UUID): GameCharacter? = players[user] as? GameCharacter

    override fun getAllPlayers(): Collection<GamePlayer> = players.values

    override fun getAllCharacters(): Collection<GameCharacter> =
        players.values.filter { it !is GameCharacter }.map { it as GameCharacter }

    override suspend fun loadUserCharactersTraits(user: UUID): UserCharactersTraits? {
        val session = sessions[user] ?: return null
        val result = session.claim.loadCharactersTraits()
        if (!result.isSuccess) {
            logger.error("Failed to load user $user characters traits", result.exceptionOrNull()!!)
            return null
        }
        return result.getOrNull()
    }
}
