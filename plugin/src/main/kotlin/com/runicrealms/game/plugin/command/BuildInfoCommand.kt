package com.runicrealms.game.plugin.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Subcommand
import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.plugin.buildinfo.RunicBuildInfoProvider
import org.bukkit.command.CommandSender

@CommandAlias("runic|r")
class BuildInfoCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val runicBuildInfoProvider: RunicBuildInfoProvider,
) : BaseCommand() {
    init {
        commandManager.registerCommand(this)
    }

    @Subcommand("buildinfo")
    fun onInfo(sender: CommandSender) {
        val info = runicBuildInfoProvider.info

        sender.sendMessage("&5[Runic] &6>> &dServer Build Info".colorFormat())
        sender.sendMessage("&7Version: &f${info.pluginVersion}".colorFormat())
        sender.sendMessage("&7Branch: &f${info.branch}".colorFormat())
        sender.sendMessage("&7Commit: &f${info.commitShortSha}".colorFormat())
        sender.sendMessage("&7Message: &f${info.commitMessage}".colorFormat())
        sender.sendMessage("&7Built: &f${info.builtAtUtc}".colorFormat())
        sender.sendMessage(
            "&7Build Source: &f${info.buildSource} &8(id: ${info.buildId})".colorFormat()
        )
    }
}
