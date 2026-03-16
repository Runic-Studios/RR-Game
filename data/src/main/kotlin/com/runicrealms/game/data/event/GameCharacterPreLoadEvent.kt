package com.runicrealms.game.data.event

import com.runicrealms.game.data.model.CharacterData
import java.util.UUID
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired after a player has chosen a character and we have confirmed the slot, but before we have
 * registered them as a [com.runicrealms.game.data.game.GameCharacter] and fired
 * [GameCharacterLoadEvent].
 *
 * Use this event to set default values for NEWLY CREATED characters. Check
 * [CharacterData.isNewCharacter] to determine whether this is a brand-new character.
 *
 * This event cannot be failed. Mutate [characterData] directly: the data class value is a reference
 * to the instance inside the in-memory document. The session manager will replace the document
 * entry after this event, so mutations here are applied before the load event.
 *
 * Note: because [CharacterData] is a data class, you cannot mutate it in place. Instead, call
 * [GameSessionManager.updateCharacterData] (or equivalent) if you need to replace fields, or hold a
 * mutable reference to the document and rebuild the map entry. In practice, handlers listening to
 * this event receive the mutable session reference via [GameSessionManager] which handles the
 * replacement after the event completes.
 */
class GameCharacterPreLoadEvent(
    val user: UUID,
    /**
     * The character data for the slot being loaded. isNewCharacter == true means first creation.
     */
    val characterData: CharacterData,
    /** The character slot index. */
    val slot: Int,
) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
