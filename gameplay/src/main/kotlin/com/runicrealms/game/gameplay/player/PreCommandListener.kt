package com.runicrealms.game.gameplay.player

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.UserDataRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.Plugin

/**
 * Prevents players from running any commands before their character has fully loaded.
 *
 * This guards against commands that rely on character data being present in [UserDataRegistry].
 */
@Singleton
class PreCommandListener
@Inject
constructor(private val plugin: Plugin, private val userDataRegistry: UserDataRegistry) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        if (userDataRegistry.getCharacter(player.uniqueId) == null) {
            event.isCancelled = true
            player.sendMessage(
                Component.text("Please wait - your character is still loading.", NamedTextColor.RED)
            )
        }
    }
}
