package com.runicrealms.game.items.event

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.character.CharacterEquipmentCache
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class GameStatUpdateEvent(
    val character: GameCharacter,
    val equipmentCache: CharacterEquipmentCache,
) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
