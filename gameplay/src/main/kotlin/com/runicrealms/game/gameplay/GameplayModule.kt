package com.runicrealms.game.gameplay

import com.google.inject.AbstractModule
import com.runicrealms.game.gameplay.tips.TipsDataListener

class GameplayModule : AbstractModule() {

    override fun configure() {
        bind(TipsDataListener::class.java).asEagerSingleton()
    }
}
