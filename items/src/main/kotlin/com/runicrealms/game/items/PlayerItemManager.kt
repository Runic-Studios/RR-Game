package com.runicrealms.game.items

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.common.event.ArmorEquipEvent
import com.runicrealms.game.data.DataAPI
import com.runicrealms.game.items.character.CharacterEquipmentCache
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import com.runicrealms.game.items.event.GameStatUpdateEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin


class PlayerItemManager @Inject constructor(
    plugin: Plugin,
    private val dataAPI: DataAPI,
): Listener, CharacterEquipmentCacheRegistry {

    override val cachedPlayerStats = ConcurrentHashMap<UUID, CharacterEquipmentCache>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Fire after other armor equip events to allow them to cancel it
    fun onArmorEquipEvent(event: ArmorEquipEvent) {
        // Sync context
        val player: Player = event.getPlayer()
        val character = dataAPI.getCharacter(player.uniqueId) ?: return
        val uuid = player.uniqueId
        if (!cachedPlayerStats.containsKey(uuid)) return
        if (event.isCancelled()) return
        val holder = cachedPlayerStats[uuid] ?: return
        when (event.type) {
            ArmorEquipEvent.ArmorType.HELMET -> holder.updateItems(false, CharacterEquipmentCache.StatHolderType.HELMET)
            ArmorEquipEvent.ArmorType.CHESTPLATE -> holder.updateItems(false, CharacterEquipmentCache.StatHolderType.CHESTPLATE)
            ArmorEquipEvent.ArmorType.LEGGINGS -> holder.updateItems(false, CharacterEquipmentCache.StatHolderType.LEGGINGS)
            ArmorEquipEvent.ArmorType.BOOTS -> holder.updateItems(false, CharacterEquipmentCache.StatHolderType.BOOTS)
            ArmorEquipEvent.ArmorType.OFFHAND -> holder.updateItems(false, CharacterEquipmentCache.StatHolderType.OFFHAND)
        }
        val statUpdateEvent = GameStatUpdateEvent(character, holder)
        Bukkit.getPluginManager().callEvent(statUpdateEvent)
    }

}