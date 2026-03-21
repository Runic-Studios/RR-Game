package com.runicrealms.game.tools

import com.google.inject.AbstractModule
import com.runicrealms.game.tools.command.TimeCommand
import com.runicrealms.game.tools.command.WarpCommand

class ToolsModule : AbstractModule() {

    override fun configure() {
        bind(WarpManager::class.java).asEagerSingleton()
        bind(WarpCommand::class.java).asEagerSingleton()
        bind(TimeCommand::class.java).asEagerSingleton()
    }
}
