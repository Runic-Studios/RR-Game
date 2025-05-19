package com.runicrealms.game.common

import co.aikar.commands.PaperCommandManager
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.runicrealms.game.common.command.CommonCompletions
import com.runicrealms.game.common.command.PaperCommandManagerProvider
import com.runicrealms.game.common.gui.InvGuiHelper

class CommonModule : AbstractModule() {

    override fun configure() {
        bind(PaperCommandManager::class.java)
            .toProvider(PaperCommandManagerProvider::class.java)
            .`in`(Scopes.SINGLETON)
        bind(CommonCompletions::class.java).asEagerSingleton()

        bind(InvGuiHelper::class.java).asEagerSingleton()
    }
}
