package com.runicrealms.game.plugin

import com.google.inject.AbstractModule
import org.bukkit.plugin.Plugin

class PluginModule(private val plugin: GamePlugin) : AbstractModule() {

    override fun configure() {
        bind(Plugin::class.java).toInstance(plugin)
        bind(GamePlugin::class.java).toInstance(plugin)
    }
}
