package com.runicrealms.game.gameplay.tips

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GamePlayerPreLoadEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class TipsDataListener
@Inject
constructor(plugin: Plugin, private val userDataRegistry: UserDataRegistry) : Listener {

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    suspend fun onGamePlayerCreate(event: GamePlayerPreLoadEvent) {
        if (!event.document.isNewPlayer) return
        userDataRegistry.getPlayer(event.user)?.withPlayerData { settings.tips = true }
    }
}
