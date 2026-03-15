package com.runicrealms.game.gameplay.data

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GamePlayerPreLoadEvent
import java.time.Instant
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class PlayerTraitDataManager
@Inject
constructor(plugin: Plugin, private val userDataRegistry: UserDataRegistry) : Listener {

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    suspend fun onGamePlayerPreLoad(event: GamePlayerPreLoadEvent) {
        userDataRegistry.getPlayer(event.user)?.withPlayerData { traits.lastLogin = Instant.now() }
    }
}
