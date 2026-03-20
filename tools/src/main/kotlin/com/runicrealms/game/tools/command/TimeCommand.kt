package com.runicrealms.game.tools.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import org.bukkit.command.CommandSender
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@CommandAlias("runic|r")
@CommandPermission("runic.op")
class TimeCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Subcommand("time")
    fun onTime(sender: CommandSender) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        sender.sendMessage("&aCurrent server time: &f$timestamp".colorFormat())
    }
}
