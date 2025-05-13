package com.runicrealms.game.items.event

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ActiveItemPerksChangeEvent(
    val character: GameCharacter,
    val oldItemPerks: Set<ItemData.Perk>,
    val newItemPerks: Set<ItemData.Perk>,
    val playSounds: Boolean
): Event(false) {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
