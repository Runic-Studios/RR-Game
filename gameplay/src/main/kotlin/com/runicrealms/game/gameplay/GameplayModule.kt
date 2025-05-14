package com.runicrealms.game.gameplay

import com.google.inject.AbstractModule
import com.runicrealms.game.gameplay.command.CharacterCommand
import com.runicrealms.game.gameplay.player.ArmorEquipListener
import com.runicrealms.game.gameplay.player.inventory.PlayerInventoryManager
import com.runicrealms.game.gameplay.tips.TipsDataListener

class GameplayModule : AbstractModule() {

    override fun configure() {
        bind(TipsDataListener::class.java).asEagerSingleton()

        bind(ArmorEquipListener::class.java).asEagerSingleton()

        bind(PlayerInventoryManager::class.java).asEagerSingleton()

        bind(CharacterCommand::class.java).asEagerSingleton()
    }
}
