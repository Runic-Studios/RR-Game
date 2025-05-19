package com.runicrealms.game.plugin

import com.google.inject.Guice
import com.runicrealms.game.common.CommonModule
import com.runicrealms.game.data.DataModule
import com.runicrealms.game.gameplay.GameplayModule
import com.runicrealms.game.items.ItemsModule
import com.runicrealms.trove.client.TroveClientConfig
import com.runicrealms.trove.client.TroveClientModule
import java.util.UUID
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory

class GamePlugin : JavaPlugin() {

    private val logger = LoggerFactory.getLogger("plugin")

    override fun onEnable() {
        val startTime = System.currentTimeMillis()

        // Setup Trove
        val troveServerID = "paper-" + UUID.randomUUID().toString().substring(0, 7)
        val troveModule = TroveClientModule(TroveClientConfig(clientName = troveServerID)) // TODO

        // Inject Guice
        val pluginModule = PluginModule(this)
        val commonModule = CommonModule()
        val dataModule = DataModule()
        val gameplayModule = GameplayModule()
        val itemsModule = ItemsModule()

        val injector =
            Guice.createInjector(
                troveModule,
                commonModule,
                pluginModule,
                dataModule,
                gameplayModule,
                itemsModule,
            )

        val time = System.currentTimeMillis() - startTime
        logger.info("Finished loading and injecting Game in $time millis")
    }
}
