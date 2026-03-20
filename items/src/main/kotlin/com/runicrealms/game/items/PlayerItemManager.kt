package com.runicrealms.game.items

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.common.event.ArmorEquipEvent
import com.runicrealms.game.data.event.GameCharacterJoinEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.items.character.CharacterEquipmentCache
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class PlayerItemManager
@Inject
constructor(
    private val plugin: Plugin,
    private val equipmentFactory: CharacterEquipmentCache.Factory,
) : Listener, CharacterEquipmentCacheRegistry {

    override val cachedCharacterStats = ConcurrentHashMap<UUID, CharacterEquipmentCache>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    fun onCharacterJoin(event: GameCharacterJoinEvent) {
        cachedCharacterStats[event.character.bukkitPlayer.uniqueId] =
            equipmentFactory.create(event.character)
    }

    @EventHandler
    fun onCharacterQuit(event: GameCharacterQuitEvent) {
        cachedCharacterStats.remove(event.character.bukkitPlayer.uniqueId)
    }

    /**
     * Paper's PlayerArmorChangeEvent fires AFTER the armor slot is committed to the inventory, so
     * reading inventory.helmet/chestplate/etc. Returns the correct new item immediately.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onArmorChange(event: EntityEquipmentChangedEvent) {
        if (event.entity !is Player) return
        val holder = cachedCharacterStats[event.entity.uniqueId] ?: return
        holder.updateAllItems(onLogin = false, callEvent = true)
    }

    /**
     * PlayerArmorChangeEvent does not cover the OFF_HAND slot, so we still listen to
     * ArmorEquipEvent for that case. A one-tick Bukkit scheduler delay ensures the off-hand slot
     * is committed before we read it.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onArmorEquipEvent(event: ArmorEquipEvent) {
        if (event.isCancelled) return
        if (event.type != ArmorEquipEvent.ArmorType.OFFHAND) return
        val holder = cachedCharacterStats[event.player.uniqueId] ?: return
        Bukkit.getScheduler().runTask(plugin) { _ -> holder.updateAllItems(onLogin = false, callEvent = true) }
    }
}
