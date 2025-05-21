package com.runicrealms.game.gameplay

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.runicrealms.game.gameplay.character.CharacterInventoryManager
import com.runicrealms.game.gameplay.command.CharacterCommand
import com.runicrealms.game.gameplay.player.ArmorEquipListener
import com.runicrealms.game.gameplay.player.charselect.CharacterAddMenu
import com.runicrealms.game.gameplay.player.charselect.CharacterDeleteMenu
import com.runicrealms.game.gameplay.player.charselect.CharacterSelectHelper
import com.runicrealms.game.gameplay.player.charselect.CharacterSelectManager
import com.runicrealms.game.gameplay.player.charselect.CharacterSelectMenu
import com.runicrealms.game.gameplay.tips.TipsDataListener
import kotlin.reflect.KClass

class GameplayModule : AbstractModule() {

    override fun configure() {
        bind(TipsDataListener::class.java).asEagerSingleton()

        bind(ArmorEquipListener::class.java).asEagerSingleton()

        bind(CharacterInventoryManager::class.java).asEagerSingleton()

        bind(CharacterCommand::class.java).asEagerSingleton()

        bind(CharacterSelectManager::class.java).asEagerSingleton()
        bind(CharacterSelectHelper::class.java).asEagerSingleton()
        addFactory(CharacterSelectMenu::class, CharacterSelectMenu.Factory::class)
        addFactory(CharacterAddMenu::class, CharacterAddMenu.Factory::class)
        addFactory(CharacterDeleteMenu::class, CharacterDeleteMenu.Factory::class)
    }

    private fun <T : Any, U : Any> addFactory(objectType: KClass<T>, factoryType: KClass<U>) {
        install(
            FactoryModuleBuilder()
                .implement(objectType.java, objectType.java)
                .build(factoryType.java)
        )
    }
}
