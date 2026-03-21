package com.runicrealms.game.gameplay.spell

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.StaffAttackEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin

@Singleton
class SpellStaffListener
@Inject
constructor(private val plugin: Plugin, private val spellManager: SpellManager) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return

        val player = event.player
        if (spellManager.getPlayerClassType(player.uniqueId) != ClassType.MAGE) return
        if (player.inventory.itemInMainHand.type == Material.AIR) return

        val staffEvent = StaffAttackEvent(player, DEFAULT_RANGE)
        Bukkit.getPluginManager().callEvent(staffEvent)
    }

    companion object {
        private const val DEFAULT_RANGE = 5
    }
}
