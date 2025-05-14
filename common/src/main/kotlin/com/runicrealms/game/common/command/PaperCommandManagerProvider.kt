package com.runicrealms.game.common.command

import co.aikar.commands.PaperCommandManager
import com.google.inject.Inject
import com.google.inject.Provider
import org.bukkit.plugin.Plugin

class PaperCommandManagerProvider @Inject constructor(private val plugin: Plugin) :
    Provider<PaperCommandManager?> {
    override fun get(): PaperCommandManager {
        return PaperCommandManager(plugin)
    }
}
