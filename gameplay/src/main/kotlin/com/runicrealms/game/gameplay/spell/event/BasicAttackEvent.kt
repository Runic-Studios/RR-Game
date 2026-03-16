package com.runicrealms.game.gameplay.spell.event

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired when a player triggers their basic attack. */
class BasicAttackEvent(
    val player: Player,
    val material: Material,
    val originalCooldownTicks: Int,
    var cooldownTicks: Double,
    var damage: Int,
    var maxDamage: Int,
) : Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        const val MINIMUM_COOLDOWN_TICKS = 5
        const val BASE_MELEE_COOLDOWN = 10
        const val BASE_BOW_COOLDOWN = 15
        const val BASE_STAFF_COOLDOWN = 15

        @JvmStatic val handlerList = HandlerList()
    }
}
