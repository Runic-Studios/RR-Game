package com.runicrealms.game.gameplay.tips

import com.google.inject.Inject
import com.runicrealms.game.data.event.GamePlayerPreLoadEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class TipsDataListener @Inject constructor(plugin: Plugin) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onGamePlayerCreate(event: GamePlayerPreLoadEvent) {
        if (!event.playerData.empty) return
        with(event.playerData) {
            settings.data.setTips(true)
            stageChanges(settings)
        }
    }
}
