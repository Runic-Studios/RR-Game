package com.runicrealms.game.plugin

import com.google.inject.Guice
import com.runicrealms.game.common.CommonModule
import com.runicrealms.game.data.DataModule
import com.runicrealms.game.gameplay.GameplayModule
import com.runicrealms.trove.client.TroveClientConfig
import com.runicrealms.trove.client.TroveClientModule
import java.util.*
import org.bukkit.plugin.java.JavaPlugin

class GamePlugin : JavaPlugin() {

    override fun onEnable() {
        val troveServerID = "paper-" + UUID.randomUUID().toString().substring(0, 7)
        val troveModule = TroveClientModule(TroveClientConfig(clientName = troveServerID)) // TODO

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
