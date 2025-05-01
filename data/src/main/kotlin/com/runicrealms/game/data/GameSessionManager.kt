package com.runicrealms.game.data

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.callSuspendingEvent
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GameSessionManager
@Inject
constructor(
    private val troveClient: TroveClient,
    private val plugin: Plugin,
) : Listener {

    private val logger = LoggerFactory.getLogger("data")

    companion object {
        const val LEASE_EXPIRY_MILLIS: Long = 60 * 1000
        const val SAVE_MILLIS: Long = 30 * 1000
    }

    private val sessions = ConcurrentHashMap<UUID, GameSession>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        val createResult = withContext(plugin.asyncDispatcher) { createSession(event.player) }
        if (!createResult.isSuccess) {
            event.player.kick(
                Component.text("Failed to load: ${createResult.exceptionOrNull()?.message}")
            )
            logger.error(
                "Failed to load player ${event.player.name}",
                createResult.exceptionOrNull()!!,
            )
            return
        }
        val session = createResult.getOrNull()!!
        sessions[event.player.uniqueId] = session

        val playerLoginEvent = GamePlayerJoinEvent(session)

        withContext(plugin.asyncDispatcher) {
            Bukkit.getPluginManager().callSuspendingEvent(playerLoginEvent, plugin).joinAll()
        }
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        withContext(plugin.asyncDispatcher) { endSession(event.player.uniqueId, true) }
    }

    private fun launchSessionSaveTask(userID: UUID): Job {
        return plugin.launch {
            withContext(plugin.asyncDispatcher) {
                while (true) {
                    val saveResult = withContext(NonCancellable) { saveSession(userID) }
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
                            saveResult.exceptionOrNull()!!,
                        )
                        // Note: kicking will trigger a save
                        break
                    }
                    delay(SAVE_MILLIS)
                }
            }
        }
    }

    private suspend fun saveSession(user: UUID): Result<Unit> {
        val session =
            sessions[user] ?: return Result.failure(IllegalStateException("Player session missing"))

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

        val createEvent = GamePlayerDataLoadEvent(player)
        Bukkit.getPluginManager().callSuspendingEvent(createEvent, plugin).joinAll()

        // This will initiate a different-context save when we run it
        val saveTask = launchSessionSaveTask(bukkitPlayer.uniqueId)

        return Result.success(GameSession(claim, player, bukkitPlayer, saveTask))
    }

    // NOTE: assumes you DON'T have a lock on the character data in the calling context!
    suspend fun setCharacterSession(session: GameSession, slot: Int?): Boolean {
        return withContext(plugin.asyncDispatcher) {
            session.characterMutex.withLock {
                val oldCharacterData = session.characterData ?: return@withLock true
                if (slot == oldCharacterData.slot) return@withLock true
                if (slot != null) {
                    val creationResult = session.claim.loadCharacter(slot)
                    if (!creationResult.isSuccess) {
                        logger.error(
                            "Failed to load character session for ${session.bukkitPlayer.uniqueId} slot $slot",
                            creationResult.exceptionOrNull()!!,
                        )
                        return@withLock false
                    }
                    val characterData = creationResult.getOrNull()!!
                    session.characterData = characterData

                    val characterJoinEvent = GameCharacterJoinEvent(session, characterData)
                    Bukkit.getPluginManager()
                        .callSuspendingEvent(characterJoinEvent, plugin)
                        .joinAll()
                } else {
                    endCharacterSession(session)
                }
                return@withLock true
            }
        }
    }

    // NOTe: assumes you have a lock on the character data in the calling context!
    private suspend fun endCharacterSession(session: GameSession) {
        val characterData = session.characterData ?: return
        val characterQuitEvent = GameCharacterQuitEvent(session, characterData)
        Bukkit.getPluginManager().callSuspendingEvent(characterQuitEvent, plugin).joinAll()
        session.characterData = null
    }

    private suspend fun endSession(user: UUID, save: Boolean) {
        val session = sessions.remove(user) ?: return

        session.saveJob.cancelAndJoin()

        session.characterMutex.withLock { endCharacterSession(session) }

        val playerQuitEvent = GamePlayerQuitEvent(session)

        Bukkit.getPluginManager().callSuspendingEvent(playerQuitEvent, plugin).joinAll()

        if (save) {
            val saveResult = saveSession(user)
            if (!saveResult.isSuccess) {
                logger.error(
                    "FATAL: failed to save user $user on session end",
                    saveResult.exceptionOrNull()!!,
                )
            }
        }

        val releaseResult = session.claim.releaseAndClose()
        if (!releaseResult.isSuccess) {
            logger.error(
                "FATAL: failed to release lock on user $user on session end",
                releaseResult.exceptionOrNull()!!,
            )
        }
    }
}
