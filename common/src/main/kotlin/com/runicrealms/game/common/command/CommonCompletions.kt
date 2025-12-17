package com.runicrealms.game.common.command

import co.aikar.commands.ConditionFailedException
import co.aikar.commands.PaperCommandManager
import com.google.inject.Inject
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

class CommonCompletions @Inject constructor(commandManager: PaperCommandManager) {

    init {
        commandManager.commandConditions.addCondition("is-console-or-op") { context ->
            if (context.issuer.issuer !is ConsoleCommandSender && !context.issuer.issuer.isOp)
            // ops can execute console commands
            throw ConditionFailedException("Only the console may run this command!")
        }
        commandManager.commandConditions.addCondition("is-op") { context ->
            if (!context.issuer.issuer.isOp)
                throw ConditionFailedException("You must be an operator to run this command!")
        }
        commandManager.commandConditions.addCondition("is-player") { context ->
            if (context.issuer.issuer !is Player)
                throw ConditionFailedException("This command cannot be run from console!")
        }
    }
}
