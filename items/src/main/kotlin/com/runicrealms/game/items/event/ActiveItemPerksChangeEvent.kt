package com.runicrealms.game.items.event

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.data.model.Perk
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ActiveItemPerksChangeEvent(
    val character: GameCharacter,
    val oldItemPerks: Collection<Perk>,
    val newItemPerks: Collection<Perk>,
    val playSounds: Boolean,
) : Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
