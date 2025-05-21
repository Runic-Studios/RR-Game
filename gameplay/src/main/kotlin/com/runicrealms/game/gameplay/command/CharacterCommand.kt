package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@CommandAlias("character|char")
class CharacterCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val userDataRegistry: UserDataRegistry,
    private val plugin: Plugin,
) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Default
    @CatchUnknown
    fun onCommand(player: Player) {
        plugin.launch {
            if (userDataRegistry.getCharacter(player.uniqueId) == null) {
                player.sendMessage("&cYou can't use this command right now.".colorFormat())
                return@launch
            }
            player.sendMessage("&aLogging you out...".colorFormat())
            val success = userDataRegistry.setCharacter(player.uniqueId, null)
            if (!success) {
                player.sendMessage(
                    "&cCould not switch your characters! Try again later.".colorFormat()
                )
            }
        }
    }
}
