package com.runicrealms.game.items.listeners

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.items.config.item.GameItemTag
import com.runicrealms.game.items.generator.ItemStackConverter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.plugin.Plugin

/**
 * Prevents dropping of items tagged as [GameItemTag.SOULBOUND]. Players in creative mode bypass
 * this restriction.
 */
class SoulboundListener
@Inject
constructor(plugin: Plugin, private val itemStackConverter: ItemStackConverter) : Listener {

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    fun onSoulboundItemDrop(event: PlayerDropItemEvent) {
        val gameItem = itemStackConverter.convertToGameItem(event.itemDrop.itemStack) ?: return
        if (!gameItem.template.tags.contains(GameItemTag.SOULBOUND)) return
        if (event.player.gameMode == GameMode.CREATIVE) return

        event.isCancelled = true
        event.player.playSound(
            event.player.location,
            Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
            0.5f,
            1.0f,
        )
        event.player.sendMessage(Component.text("This item is soulbound.", NamedTextColor.GRAY))
    }
}
