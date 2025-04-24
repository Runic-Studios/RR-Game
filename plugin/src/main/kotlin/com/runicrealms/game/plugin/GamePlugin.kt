package com.runicrealms.game.plugin

import com.google.inject.Guice
import com.runicrealms.game.common.CommonModule
import com.runicrealms.game.data.DataModule
import com.runicrealms.game.gameplay.GameplayModule
import com.runicrealms.trove.client.TroveClientConfig
import com.runicrealms.trove.client.TroveClientModule
import org.bukkit.plugin.java.JavaPlugin

class GamePlugin : JavaPlugin() {

    override fun onEnable() {
        val troveModule = TroveClientModule(TroveClientConfig("localhost", 9091, "TODO")) // TODO

        val pluginModule = PluginModule(this)
        val commonModule = CommonModule()
        val dataModule = DataModule()
        val gameplayModule = GameplayModule()

        val injector =
            Guice.createInjector(
                troveModule,
                commonModule,
                pluginModule,
                dataModule,
                gameplayModule,
            )
    }
}
