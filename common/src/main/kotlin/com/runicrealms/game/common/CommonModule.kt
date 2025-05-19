package com.runicrealms.game.common

import co.aikar.commands.PaperCommandManager
import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Scopes
import com.runicrealms.game.common.command.CommonCompletions
import nl.odalitadevelopments.menus.OdalitaMenus
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class CommonModule : AbstractModule() {

    override fun configure() {
        bind(PaperCommandManager::class.java)
            .toProvider(PaperCommandManagerProvider::class.java)
            .`in`(Scopes.SINGLETON)
        bind(CommonCompletions::class.java).asEagerSingleton()

        bind(OdalitaMenus::class.java)
            .toProvider(OdalitaMenusProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }

    private class OdalitaMenusProvider @Inject constructor(private val plugin: JavaPlugin) :
        Provider<OdalitaMenus> {
        override fun get(): OdalitaMenus {
            return OdalitaMenus.getInstance(plugin)
        }
    }

    private class PaperCommandManagerProvider @Inject constructor(private val plugin: Plugin) :
        Provider<PaperCommandManager?> {
        override fun get(): PaperCommandManager {
            return PaperCommandManager(plugin)
        }
    }
}
