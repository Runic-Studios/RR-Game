package com.runicrealms.game.gameplay.spell.event

import com.runicrealms.game.gameplay.spell.spelltypes.SpellSlot
import com.runicrealms.game.gameplay.spell.spelltypes.SpellTriggerType
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired when a player triggers the two-step spell cast UI.
 *
 * The [spellTriggerType] is computed from the player's current class: Archer uses
 * [SpellTriggerType.ARCHER]; all other classes use [SpellTriggerType.DEFAULT].
 *
 * TODO: Replace the class lookup with GameCharacter once SpellUseListener has access to it.
 */
class SpellTriggerEvent(
    val player: Player,
    val spellSlot: SpellSlot,
    val spellTriggerType: SpellTriggerType,
) : Event(), Cancellable {

    private var cancelled = false

    /** Secondary gate for tier sets that want to suppress execution without full cancellation. */
    var willExecute: Boolean = true

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
