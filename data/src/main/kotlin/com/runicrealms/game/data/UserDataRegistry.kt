package com.runicrealms.game.data

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.data.game.GamePlayer
import com.runicrealms.trove.client.user.UserCharactersTraits
import java.util.UUID

interface UserDataRegistry {

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
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread. Calling from
     * async threads can result in undetermined behaviour.
     */
    fun getPlayer(user: UUID): GamePlayer?

    /**
     * Gets a GameCharacter from their UUID.
     *
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread. Calling from
     * async threads can result in undetermined behaviour.
     */
    fun getCharacter(user: UUID): GameCharacter?

    /**
     * Gets all logged-in GamePlayers.
     *
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread. Calling from
     * async threads can result in undetermined behaviour.
     */
    fun getAllPlayers(): Collection<GamePlayer>

    /**
     * Gets all logged-in GameCharacters.
     *
     * It is STRONGLY ADVISED that this only be called on the Minecraft game thread. Calling from
     * async threads can result in undetermined behaviour.
     */
    fun getAllCharacters(): Collection<GameCharacter>

    /**
     * Loads the character traits for all slots for a given player. Does not fire any character load
     * events. Returns an empty map if none exist.
     *
     * The purpose of this method is to grab info on the character
     */
    suspend fun loadUserCharactersTraits(user: UUID): UserCharactersTraits?
}
