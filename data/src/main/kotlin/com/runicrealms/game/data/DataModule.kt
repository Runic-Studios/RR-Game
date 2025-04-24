package com.runicrealms.game.data

import com.google.inject.AbstractModule

class DataModule : AbstractModule() {

    override fun configure() {
        bind(GameSessionManager::class.java).asEagerSingleton()
    }
}
