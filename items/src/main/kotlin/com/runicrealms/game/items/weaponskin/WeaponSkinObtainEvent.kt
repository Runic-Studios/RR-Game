package com.runicrealms.game.items.weaponskin

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired when a player obtains a new weapon skin. */
class WeaponSkinObtainEvent(val player: Player, val skin: WeaponSkin) : Event() {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
