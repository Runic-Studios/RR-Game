package com.runicrealms.game.data

import com.google.inject.AbstractModule
import com.runicrealms.game.data.game.GameSessionManager

class DataModule : AbstractModule() {

    override fun configure() {
        bind(GameSessionManager::class.java).asEagerSingleton()
        bind(UserDataRegistry::class.java).to(GameSessionManager::class.java)
    }
}
