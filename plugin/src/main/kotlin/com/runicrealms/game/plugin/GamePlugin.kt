package com.runicrealms.game.plugin

import com.google.inject.Guice
import com.runicrealms.game.common.CommonModule
import com.runicrealms.game.data.DataModule
import com.runicrealms.game.data.config.MongoModule
import com.runicrealms.game.gameplay.GameplayModule
import com.runicrealms.game.items.ItemsModule
import java.io.File
import java.util.UUID
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory

class GamePlugin : JavaPlugin() {

    private val logger = LoggerFactory.getLogger("plugin")

    override fun onEnable() {
        val startTime = System.currentTimeMillis()

        // Save default config.yml to plugins/Game/config.yml on first run
        saveDefaultConfig()

        val databaseName =
            config.getString("database-name")
                ?: throw IllegalStateException(
                    "Missing required config value 'database-name' in config.yml"
                )

        // mongodb.secret lives in the server root, one level above the plugins/ folder
        val serverRoot = dataFolder.parentFile.parentFile
        val secretFile = File(serverRoot, "mongodb.secret")
        val connectionString =
            secretFile.takeIf { it.exists() }?.readText()?.trim()
                ?: throw IllegalStateException(
                    "mongodb.secret not found at ${secretFile.absolutePath}"
                )

        // Each server instance gets a unique ID used as the distributed lock owner.
        val serverId = "paper-" + UUID.randomUUID().toString().substring(0, 7)

        val mongoModule = MongoModule(connectionString, databaseName, serverId)
        val pluginModule = PluginModule(this)
        val commonModule = CommonModule()
        val dataModule = DataModule()
        val gameplayModule = GameplayModule()
        val itemsModule = ItemsModule()

        Guice.createInjector(
            mongoModule,
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
