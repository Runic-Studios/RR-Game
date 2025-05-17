package com.runicrealms.game.items.command

import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import com.google.inject.Inject
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.items.GameItemManager
import org.bukkit.command.CommandSender

@CommandAlias("runic|r")
@Subcommand("reload items")
@CommandPermission("runic.op")
class ReloadItemsCommand @Inject constructor(private val gameItemManager: GameItemManager) {

    @CatchUnknown
    @Default
    fun onCommand(sender: CommandSender) {
        // NOTE: Even if you have deleted items/perks, this command never removes them.
        // It will only amend existing config or add new ones.
        sender.sendMessage("&aReloading custom items, script items, and perks...".colorFormat())
        gameItemManager.readConfig()
        sender.sendMessage("&aDone reloading items!".colorFormat())
    }
}
