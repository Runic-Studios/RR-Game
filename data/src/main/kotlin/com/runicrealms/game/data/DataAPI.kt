package com.runicrealms.game.data

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.data.game.GamePlayer
import java.util.UUID

interface DataAPI {

    /**
     * Sets a user's active character.
     *
     * @param slot Which character slot to change to, null if to log-out
     * @return Success
     */
    suspend fun setCharacter(user: UUID, slot: Int?): Boolean

    /**
     * Gets a GamePlayer from their UUID.
     *
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread.
     * Calling from async threads can result in undetermined behaviour.
     */
    fun getPlayer(user: UUID): GamePlayer?

    /**
     * Gets a GameCharacter from their UUID.
     *
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread.
     * Calling from async threads can result in undetermined behaviour.
     */
    fun getCharacter(user: UUID): GameCharacter?

    /**
     * Gets all logged-in GamePlayers.
     *
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread.
     * Calling from async threads can result in undetermined behaviour.
     */
    fun getAllPlayers(): Collection<GamePlayer>

    /**
     * Gets all logged-in GameCharacters.
     *
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread.
     * Calling from async threads can result in undetermined behaviour.
     */
    fun getAllCharacters(): Collection<GameCharacter>

}