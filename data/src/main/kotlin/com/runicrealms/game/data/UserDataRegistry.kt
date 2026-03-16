package com.runicrealms.game.data

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.data.game.GamePlayer
import com.runicrealms.game.data.model.CharacterTraits
import java.util.UUID

interface UserDataRegistry {

    /**
     * Switches the active character for [user] to [slot], or clears the active character if [slot]
     * is null (returning the player to the character selection screen).
     *
     * Must be called on the Minecraft main thread.
     *
     * @return true if the switch succeeded, false if an event handler failed the load.
     */
    suspend fun setCharacter(user: UUID, slot: Int?): Boolean

    /**
     * Returns the [GamePlayer] for [user] if they are currently online, or null.
     *
     * Must be called on the Minecraft main thread to avoid race conditions with join/quit events
     * that mutate the player map.
     */
    fun getPlayer(user: UUID): GamePlayer?

    /**
     * Returns the [GameCharacter] for [user] if they have an active character, or null.
     *
     * Must be called on the Minecraft main thread.
     */
    fun getCharacter(user: UUID): GameCharacter?

    /**
     * Returns all currently online [GamePlayer]s (including those on the character select screen).
     *
     * Must be called on the Minecraft main thread.
     */
    fun getAllPlayers(): Collection<GamePlayer>

    /**
     * Returns all currently online players who have an active character selected.
     *
     * Must be called on the Minecraft main thread.
     */
    fun getAllCharacters(): Collection<GameCharacter>

    /**
     * Returns a snapshot of the [CharacterTraits] for every character slot of [user].
     *
     * Because all characters are loaded at login, this is a pure in-memory read with no DB call.
     * Returns null if the player is not currently online.
     */
    fun loadUserCharactersTraits(user: UUID): Map<Int, CharacterTraits>?
}
