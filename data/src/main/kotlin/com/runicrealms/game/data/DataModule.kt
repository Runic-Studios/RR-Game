package com.runicrealms.game.data

import com.google.inject.AbstractModule
import com.runicrealms.game.data.game.GameSessionManager
import com.runicrealms.game.data.menu.GameCharacterMenuProviderLoader
import com.runicrealms.game.data.menu.GameMenuProviderRegistry
import com.runicrealms.game.data.menu.GamePlayerMenuProviderLoader

class DataModule : AbstractModule() {

    override fun configure() {
        bind(GameSessionManager::class.java).asEagerSingleton()
        bind(UserDataRegistry::class.java).to(GameSessionManager::class.java)

        bind(GameCharacterMenuProviderLoader::class.java).asEagerSingleton()
        bind(GamePlayerMenuProviderLoader::class.java).asEagerSingleton()
        bind(GameMenuProviderRegistry::class.java).asEagerSingleton()
    }
}
